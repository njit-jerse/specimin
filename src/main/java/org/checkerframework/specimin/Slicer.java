package org.checkerframework.specimin;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.specimin.unsolved.UnsolvedGenerationResult;
import org.checkerframework.specimin.unsolved.UnsolvedSymbolAlternates;
import org.checkerframework.specimin.unsolved.UnsolvedSymbolGenerator;

/**
 * Slices a program, given an initial worklist and a type rule dependency map. This class cannot be
 * instantiated; instead, use {@link #slice(TypeRuleDependencyMap, Deque, UnsolvedSymbolGenerator,
 * Map)} to use this class.
 */
public class Slicer {
  /**
   * The slice of nodes.
   *
   * <p>We cannot use HashSet<Node> here; when nodes' children are modified, {@code slice.contains}
   * no longer returns true, even if {@code equals} evaluates to true, {@code hashCode} returns the
   * same value, and {@code ==} yields the same reference.
   *
   * <p>An {@code IdentityHashMap} is safe to use because we use the same compilation units
   * everywhere, so all corresponding nodes will have the same reference.
   */
  private final Set<Node> slice = Collections.newSetFromMap(new IdentityHashMap<>());

  /** The slice of generated symbols. */
  private final Set<UnsolvedSymbolAlternates<?>> generatedSymbolSlice = new LinkedHashSet<>();

  /** The Nodes that need to be processed in the main algorithm. */
  private final Deque<Node> worklist;

  /**
   * A secondary worklist to use to add information to generated symbols, once all unsolved symbols
   * are guaranteed to be generated. Contains only select Nodes, determined by {@link
   * UnsolvedSymbolGenerator#needToPostProcess(Node)}.
   */
  private final Deque<Node> postProcessingWorklist = new ArrayDeque<>();

  /** The result of the slice, not including generated symbols. */
  private final Set<CompilationUnit> resultCompilationUnits = new HashSet<>();

  private final UnsolvedSymbolGenerator unsolvedSymbolGenerator;
  private final TypeRuleDependencyMap typeRuleDependencyMap;

  private final Map<String, CompilationUnit> fqnToCompilationUnits;

  /**
   * Creates a new instance of {@link Slicer}.
   *
   * @param typeRuleDependencyMap The type rule dependency map to use in the slice.
   * @param worklist The worklist to use, already populated with target members and their bodies.
   * @param unsolvedSymbolGenerator The unsolved symbol generator to use.
   * @param fqnToCompilationUnits The fully qualified name to compilation units map.
   */
  private Slicer(
      TypeRuleDependencyMap typeRuleDependencyMap,
      Deque<Node> worklist,
      UnsolvedSymbolGenerator unsolvedSymbolGenerator,
      Map<String, CompilationUnit> fqnToCompilationUnits) {
    this.typeRuleDependencyMap = typeRuleDependencyMap;
    this.worklist = worklist;
    this.unsolvedSymbolGenerator = unsolvedSymbolGenerator;
    this.fqnToCompilationUnits = fqnToCompilationUnits;
  }

  /**
   * Slices a program based on the type rule dependency map and an initial worklist. Returns a
   * {@link SliceResult} which contains sliced compilation units and also generated unsolved
   * symbols.
   *
   * @param typeRuleDependencyMap The type rule dependency map to use in the slice.
   * @param worklist The worklist to use, already populated with target members and their bodies.
   * @return A {@link SliceResult} representing the output of the slice.
   */
  public static SliceResult slice(
      TypeRuleDependencyMap typeRuleDependencyMap,
      Deque<Node> worklist,
      UnsolvedSymbolGenerator unsolvedSymbolGenerator,
      Map<String, CompilationUnit> fqnToCompilationUnits) {
    Slicer slicer =
        new Slicer(typeRuleDependencyMap, worklist, unsolvedSymbolGenerator, fqnToCompilationUnits);

    slicer.buildSlice();

    Set<Node> dependentSlice = new HashSet<>();
    for (UnsolvedSymbolAlternates<?> gen : slicer.generatedSymbolSlice) {
      for (Node node : gen.getDependentNodes()) {
        // These nodes may be something like a method declaration; we need to call the
        // type rule dependency map once to get its keywords to make sure it's not removed
        slicer.slice.add(node);
        slicer.slice.addAll(typeRuleDependencyMap.getRelevantElements(node));

        if (!slicer.slice.contains(node)) dependentSlice.add(node);
      }
    }

    unsolvedSymbolGenerator.generateAllAlternatesBasedOnSuperTypeRelationships();

    slicer.prune();

    return new SliceResult(
        slicer.resultCompilationUnits, slicer.generatedSymbolSlice, dependentSlice);
  }

  /**
   * The main slicing algorithm. Mutates the compilation units in {@link #resultCompilationUnits}
   * and trims all unused nodes, while also adding needed generated symbols to the result.
   */
  private void buildSlice() {
    // Step 1: build the slice; see which nodes to keep
    while (!worklist.isEmpty()) {
      Node element = worklist.removeLast();
      handleElement(element);
    }

    generatedSymbolSlice.addAll(unsolvedSymbolGenerator.clearMethodsWithNull());

    if (!generatedSymbolSlice.isEmpty()) {
      // Step 2: Add more information to generated symbols based on context
      Iterator<Node> ppwIterator = postProcessingWorklist.iterator();
      while (ppwIterator.hasNext()) {
        Node element = ppwIterator.next();
        UnsolvedGenerationResult result = unsolvedSymbolGenerator.addInformation(element);
        generatedSymbolSlice.addAll(result.toAdd());
        generatedSymbolSlice.removeAll(result.toRemove());
      }
    }
  }

  /** Prunes all unused elements based on the slice. */
  private void prune() {
    // Step 3: go through each compilation unit and remove unused nodes
    for (CompilationUnit cu : resultCompilationUnits) {
      // If a non-primary class is preserved, the primary class still must be preserved,
      // even if all its nodes were removed
      slice.add(cu.getPrimaryType().get());

      removeNonSliceNodesFromCompilationUnit(cu);
    }
  }

  /**
   * Helper method for each node in the worklist. Generates a symbol if needed, otherwise it simply
   * adds it and the results of a call to the type rule dependency map to the slice.
   *
   * @param node The node to handle
   */
  private void handleElement(Node node) {
    if (slice.contains(node)) {
      return;
    }

    // TypeParameter#resolve() throws an UnsupportedOperationException.
    // Since we don't need to resolve the type parameter definition, it is
    // safe to skip this step, since all child nodes will be added to the
    // worklist anyway.

    // UnknownType#resolve() throws an IllegalArgumentException (see docs,
    // #convertToUsage(Context)).
    // https://www.javadoc.io/doc/com.github.javaparser/javaparser-core/latest/com/github/javaparser/ast/type/UnknownType.html
    if (node instanceof Resolvable<?>
        && !(node instanceof TypeParameter || node instanceof UnknownType)) {
      Resolvable<?> asResolvable = (Resolvable<?>) node;

      boolean generateUnsolvedSymbol = false;
      Object resolved = null;
      try {
        // Do not call resolve on a FieldDeclaration, since it will throw an
        // UnsupportedOperationException
        // if the field has multiple declarators. We will resolve the declarators instead.
        if (!(node instanceof FieldDeclaration)) {
          // Resolve isn't perfect: methods/constructors, even if in the same file, will not resolve
          // if there are unresolvable argument types
          resolved = asResolvable.resolve();
        }
      } catch (UnsupportedOperationException ex) {
        // java.lang.UnsupportedOperationException: The type declaration cannot be found on
        // constraint T
        // Workaround for resolving methods/fields with a qualifier that is resolvable, but returns
        // a lambda constraint type with a type parameter instead of a type

        // If not this case, then throw the error.
        if (!(node instanceof Expression)) {
          throw ex;
        }

        resolved =
            JavaParserUtil.tryFindCorrespondingDeclarationForConstraintQualifiedExpression(
                (Expression) node);

        if (resolved == null) {
          throw ex;
        }
      } catch (UnsolvedSymbolException ex) {
        boolean shouldTryToResolve = true;
        if (node instanceof ClassOrInterfaceType type && JavaParserUtil.isProbablyAPackage(type)) {
          // We may encounter this if the user includes a FQN in their input, since the type rule
          // dependency map returns the scope of the type, even if it's a package.
          shouldTryToResolve = false;
          generateUnsolvedSymbol = false;
        }

        // Handle cases where a method/constructor call cannot be resolved because of unresolvable
        // arguments, but its definition exists
        Resolvable<?> potentiallyResolvableCallable =
            node instanceof NodeWithArguments<?> withArgs
                ? tryResolveNodeWithUnsolvableArguments(withArgs)
                : null;

        if (potentiallyResolvableCallable != null) {
          try {
            resolved = potentiallyResolvableCallable.resolve();
            shouldTryToResolve = false;
          } catch (UnsolvedSymbolException e) {
            // This should never happen
          }
        }

        if (shouldTryToResolve) {
          // Calling resolve on a FieldAccessExpr/NameExpr that represents a type may also cause
          // an UnsolvedSymbolException, even if the type is resolvable
          if (node instanceof FieldAccessExpr || node instanceof NameExpr) {
            if (!JavaParserUtil.isProbablyAPackage((Expression) node)) {
              try {
                resolved = ((Expression) node).calculateResolvedType();
              } catch (UnsolvedSymbolException ex2) {
                generateUnsolvedSymbol = true;
              }
            } else {
              generateUnsolvedSymbol = false;
            }
          } else {
            generateUnsolvedSymbol = true;
          }
        }

        if (generateUnsolvedSymbol && node instanceof Expression expr) {
          Object result = JavaParserUtil.tryResolveExpressionIfInAnonymousClass(expr);
          if (result != null) {
            resolved = result;
            generateUnsolvedSymbol = false;
          }
        }
      }

      if (resolved != null) {
        generateUnsolvedSymbol = handleResolvedObject(node, resolved);
      }

      if (generateUnsolvedSymbol) {
        generatedSymbolSlice.addAll(unsolvedSymbolGenerator.inferContext(node));
      }
    }

    slice.add(node);
    worklist.addAll(typeRuleDependencyMap.getRelevantElements(node));

    if (unsolvedSymbolGenerator.needToPostProcess(node)) {
      postProcessingWorklist.add(node);
    }
  }

  /**
   * Tries to resolve a node with unresolvable arguments. If no single callable can be found, it
   * returns null.
   *
   * @param node The node with arguments to resolve
   * @return A resolvable callable if it can be found, null otherwise
   */
  private @Nullable Resolvable<?> tryResolveNodeWithUnsolvableArguments(NodeWithArguments<?> node) {
    List<? extends CallableDeclaration<?>> callables;

    if (node instanceof MethodCallExpr methodCall) {
      callables =
          JavaParserUtil.tryResolveMethodCallWithUnresolvableArguments(
              methodCall, fqnToCompilationUnits);
    } else if (node instanceof ObjectCreationExpr objectCreationExpr) {
      callables =
          JavaParserUtil.tryResolveConstructorCallWithUnresolvableArguments(
              objectCreationExpr, fqnToCompilationUnits);
    } else if (node instanceof ExplicitConstructorInvocationStmt constructorCall) {
      callables =
          JavaParserUtil.tryResolveConstructorCallWithUnresolvableArguments(
              constructorCall, fqnToCompilationUnits);
    } else if (node instanceof EnumConstantDeclaration enumConstant) {
      callables =
          JavaParserUtil.tryResolveEnumConstantDeclarationWithUnresolvableArguments(
              enumConstant, fqnToCompilationUnits);
    } else {
      return null;
    }

    if (callables.isEmpty()) {
      return null;
    }

    if (callables.size() == 1) {
      return (Resolvable<?>) callables.get(0);
    }

    List<@Nullable Object> argumentTypes =
        new ArrayList<>(JavaParserUtil.getArgumentTypesAsResolved(node.getArguments()));

    for (int i = 0; i < argumentTypes.size(); i++) {
      if (argumentTypes.get(i) == null) {
        argumentTypes.set(
            i,
            JavaParserUtil.tryGetTypeAsStringFromExpression(
                node.getArgument(i), fqnToCompilationUnits));
      }
    }

    // If there is only one callable where the rest of the parameters match and a few others that
    // are null, return this maybe best match
    CallableDeclaration<?> maybeBestMatch = null;
    // If there are multiple callables, find the best match, i.e., FQNs = FQNs and simple names =
    // simple names
    // If there is one that matches exactly, return it; others that match it directly are simply
    // overrides
    for (CallableDeclaration<?> callable : callables) {
      boolean isAMatch = true;
      int nulls = 0;
      for (int i = 0; i < callable.getParameters().size(); i++) {
        Object typeInCall = argumentTypes.get(i);

        if (typeInCall == null) {
          nulls++;
          continue;
        }

        // The call to tryResolve... already guarantees that ResolvedType is handled correctly
        if (typeInCall instanceof ResolvedType) {
          continue;
        }

        Type paramType = callable.getParameter(i).getType();
        if (typeInCall instanceof String typeAsString) {
          // If the type in the call is a string, it must match the parameter type exactly
          if (!JavaParserUtil.erase(paramType.asString())
              .equals(JavaParserUtil.erase(typeAsString))) {
            isAMatch = false;
            break;
          }
        }
      }

      if (isAMatch) {
        if (nulls == 0) {
          // If there are no nulls, this is a perfect match
          return (Resolvable<?>) callable;
        } else if (maybeBestMatch == null) {
          // If there are nulls, this is the best match so far
          maybeBestMatch = callable;
        } else {
          // If nulls > 0 and maybeBestMatch is already existing, return null since we have
          // ambiguities: handle in UnsolvedSymbolGenerator
          return null;
        }
      }
    }

    return (Resolvable<?>) maybeBestMatch;
  }

  /**
   * Handles a resolved Object returned by {@code Resolvable<?>.resolve()}. Helper method to be used
   * by {@link #handleElement(Node)}.
   *
   * @param resolved The resolved object
   * @return true if an unsolved symbol must be generated from this node (if a method is marked with
   *     {@code @Override} but no override is found); false otherwise
   */
  private boolean handleResolvedObject(Node unresolved, @Nullable Object resolved) {
    if (resolved == null) {
      throw new RuntimeException("Unexpected null value in resolve() call");
    }

    List<Node> toAddToWorklist = typeRuleDependencyMap.getRelevantElements(resolved);
    worklist.addAll(toAddToWorklist);

    // Since resolved declarations may reference another file, we need to add that compilation
    // unit to the output
    resultCompilationUnits.addAll(
        toAddToWorklist.stream().map(n -> n.findCompilationUnit().get()).toList());

    if (unresolved instanceof MethodDeclaration methodDecl
        && (methodDecl.getAnnotationByName("Override").isPresent()
            || (methodDecl.getParentNode().orElse(null) instanceof ObjectCreationExpr
                && methodDecl.isPublic()))) {
      return !toAddToWorklist.stream()
          .anyMatch(n -> n instanceof MethodDeclaration && !n.equals(methodDecl));
    }

    return false;
  }

  /**
   * Removes unused nodes from a compilation unit. Use this instead of {@link
   * #removeNonSliceNodes(Node)} because {@link CompilationUnit} has some quirks that prevents
   * {@link CompilationUnit#getChildNodes()} from accessing anything but package/import
   * declarations.
   *
   * @param cu The compilation unit
   */
  private void removeNonSliceNodesFromCompilationUnit(CompilationUnit cu) {
    List<TypeDeclaration<?>> typesCopy = new ArrayList<>(cu.getTypes());
    for (TypeDeclaration<?> typeDecl : typesCopy) {
      removeNonSliceNodes(typeDecl);
    }
  }

  /**
   * Recursively removes all nodes not in the slice.
   *
   * @param node The node to slice
   */
  private void removeNonSliceNodes(Node node) {
    if (slice.contains(node)) {
      List<Node> copy = new ArrayList<>(node.getChildNodes());
      for (Node child : copy) {
        removeNonSliceNodes(child);
      }

      // If we encounter an empty final field, try to see if we've included an assignment expression
      // that sets it in the slice. If not, then we need to add a default initializer.
      if (node instanceof VariableDeclarator fieldDeclarator
          && fieldDeclarator.getInitializer().isEmpty()
          && fieldDeclarator.getParentNode().get() instanceof FieldDeclaration fieldDecl
          && fieldDecl.isFinal()) {
        ResolvedFieldDeclaration resolved = (ResolvedFieldDeclaration) fieldDeclarator.resolve();

        boolean isSet =
            slice.stream()
                .filter(n -> n instanceof AssignExpr)
                .map(n -> (AssignExpr) n)
                .filter(
                    assignExpr -> {
                      if (assignExpr.getTarget().isFieldAccessExpr()) {
                        try {
                          ResolvedValueDeclaration target =
                              assignExpr.getTarget().asFieldAccessExpr().resolve();

                          if (target.isField()) {
                            return target
                                    .asField()
                                    .declaringType()
                                    .getQualifiedName()
                                    .equals(resolved.declaringType().getQualifiedName())
                                && target.getName().equals(resolved.getName());
                          }
                        } catch (UnsolvedSymbolException e) {
                          return false;
                        }
                      } else if (assignExpr.getTarget().isNameExpr()) {
                        try {
                          ResolvedValueDeclaration target =
                              assignExpr.getTarget().asNameExpr().resolve();

                          if (target.isField()) {
                            return target
                                    .asField()
                                    .declaringType()
                                    .getQualifiedName()
                                    .equals(resolved.declaringType().getQualifiedName())
                                && target.getName().equals(resolved.getName());
                          }
                          return target.equals(resolved);
                        } catch (UnsolvedSymbolException e) {
                          return false;
                        }
                      }

                      return false;
                    })
                .findFirst()
                .isPresent();

        if (!isSet) {
          fieldDeclarator.setInitializer(
              JavaParserUtil.getInitializerRHS(fieldDeclarator.getType().toString()));
        }
      }
    }
    // If a BlockStmt is being removed, it's a method/constructor to be trimmed
    else if (node instanceof BlockStmt blockStmt
        && node.getParentNode().get() instanceof CallableDeclaration<?> callable) {
      TypeDeclaration<?> enclosing = JavaParserUtil.getEnclosingClassLike(node);

      boolean handled = false;
      if (enclosing.isClassOrInterfaceDeclaration() && callable.isMethodDeclaration()) {
        // Non-default interface method
        if (enclosing.asClassOrInterfaceDeclaration().isInterface()
            && !callable.asMethodDeclaration().isDefault()) {
          handled = true;
          node.remove();
        }
        // abstract method
        if (callable.asMethodDeclaration().isAbstract()) {
          handled = true;
          node.remove();
        }
      }
      if (!handled) {
        blockStmt.setStatements(
            new NodeList<>(StaticJavaParser.parseStatement("throw new java.lang.Error();")));
      }
    }
    // If an initializer is being removed, it's a field declaration to be trimmed
    // Only replace with default value if it's a final field
    else if (node instanceof Expression initializer
        && node.getParentNode().get() instanceof VariableDeclarator fieldDeclarator
        && fieldDeclarator.getInitializer().isPresent()
        && fieldDeclarator.getInitializer().get().equals(initializer)
        && fieldDeclarator.getParentNode().get() instanceof FieldDeclaration fieldDecl
        && fieldDecl.isFinal()) {
      fieldDeclarator.setInitializer(
          JavaParserUtil.getInitializerRHS(fieldDeclarator.getType().toString()));
    } else {
      node.remove();
    }
  }

  /**
   * Represents the result of a slice.
   *
   * @param solvedSlice A set of compilation units, with all unused nodes trimmed, representing the
   *     slice of solved elements.
   * @param generatedSymbolSlice A set of all generated symbols that must be included in the final
   *     output.
   */
  public record SliceResult(
      Set<CompilationUnit> solvedSlice,
      Set<UnsolvedSymbolAlternates<?>> generatedSymbolSlice,
      Set<Node> generatedSymbolDependentSlice) {
    // Override getter methods so we can add javadoc

    /**
     * Gets each used compilation unit with all its unused nodes removed; does not include generated
     * symbols.
     *
     * @return The result of the slice
     */
    @Override
    public Set<CompilationUnit> solvedSlice() {
      return solvedSlice;
    }

    /**
     * Gets all generated symbols that must be included in the final output.
     *
     * @return A set of unsolved symbols generated during the slice.
     */
    @Override
    public Set<UnsolvedSymbolAlternates<?>> generatedSymbolSlice() {
      return generatedSymbolSlice;
    }

    /**
     * Gets all nodes that are dependent on at least one alternate and not part of the "mandatory"
     * slice.
     *
     * @return All nodes that are only dependent on an alternate.
     */
    @Override
    public Set<Node> generatedSymbolDependentSlice() {
      return generatedSymbolDependentSlice;
    }
  }
}
