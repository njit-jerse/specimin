package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.PatternExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.specimin.JavaLangUtils;
import org.checkerframework.specimin.JavaParserUtil;

/**
 * Generates unsolved symbols. This class ensures that only one of each type is created; i.e., the
 * same FQNs will point to the same instance. More symbols are tracked here than returned into the
 * final slice; this is to ensure classes used by some alternates are only outputted when those
 * alternates are selected.
 */
public class UnsolvedSymbolGenerator {
  private final Map<String, CompilationUnit> fqnsToCompilationUnits;
  private final FullyQualifiedNameGenerator fullyQualifiedNameGenerator;

  /**
   * Creates a new UnsolvedSymbolGenerator. Pass in a set of fqns to compilation units for
   * resolution purposes.
   *
   * @param fqnsToCompilationUnits A set of fully-qualified names to compilation units
   */
  public UnsolvedSymbolGenerator(Map<String, CompilationUnit> fqnsToCompilationUnits) {
    this.fqnsToCompilationUnits = fqnsToCompilationUnits;
    fullyQualifiedNameGenerator = new FullyQualifiedNameGenerator(fqnsToCompilationUnits);
  }

  /**
   * The cache of unsolved symbol definitions. These values need not be unique; the map is provided
   * for simple lookups when adding new symbols. Keys: fully qualified names --> values: unsolved
   * symbol alternates
   */
  private final Map<String, UnsolvedSymbolAlternates<?>> generatedSymbols = new HashMap<>();

  /**
   * Given an unresolvable Node, generate a corresponding synthetic definition. In cases where
   * multiple nodes are not known (for example, the node is a field A.b and both type A and field b
   * are not resolvable), this method will recursively call itself and return both generated
   * symbols.
   *
   * @param node The unresolvable node
   * @return A list of UnsolvedSymbolAlternates generated/found from the input
   */
  public List<UnsolvedSymbolAlternates<?>> inferContext(Node node) {
    List<UnsolvedSymbolAlternates<?>> generated = new ArrayList<>();
    inferContextImpl(node, generated);

    return generated;
  }

  /**
   * Unsolved symbols are added to result. The member generated/found based on {@code node} is added
   * in addition to any types in its scope. Only items that must be included in the final output
   * should be added to result.
   *
   * @param node The node
   * @param result The list of generated/found symbols, according to the rules above
   */
  private void inferContextImpl(Node node, List<UnsolvedSymbolAlternates<?>> result) {
    // https://www.javadoc.io/doc/com.github.javaparser/javaparser-core/latest/com/github/javaparser/resolution/Resolvable.html

    // Ignore declarations in this method. If a declaration is not resolvable, it's probably because
    // a member is not resolvable. But, the type dependency map will eventually reach it, so the
    // symbol will eventually be generated anyway.

    // Also ignore nodes like ArrayType or IntersectionType because the type rule dependency map
    // will also break down its types.

    // Types
    if (node instanceof ClassOrInterfaceType asType) {
      handleClassOrInterfaceType(asType, result);
    } else if (node instanceof AnnotationExpr asAnno) {
      handleAnnotationExpr(asAnno, result);
    }
    // Fields (although some types are handled as FieldAccessExpr or NameExpr too)
    else if (node instanceof FieldAccessExpr asField) {
      handleFieldAccessExpr(asField, result);
    } else if (node instanceof NameExpr nameExpr) {
      handleNameExpr(nameExpr, result);
    }
    // Methods
    else if (node instanceof MethodCallExpr methodCall) {
      handleMethodCallExpr(methodCall, result);
    } else if (node instanceof ObjectCreationExpr
        || node instanceof ExplicitConstructorInvocationStmt) {
      UnsolvedClassOrInterfaceAlternates scope;
      String constructorName;
      List<Expression> arguments;
      int numberOfTypeParams = 0;

      if (node instanceof ObjectCreationExpr constructor) {
        try {
          constructor.calculateResolvedType();
          // If the type is resolvable, the constructor is too; a type in the constructor is not
          // solvable. Return because we don't need to generate a new constructor.
          return;
        } catch (UnsolvedSymbolException ex) {
          // continue
        }

        inferContextImpl(constructor.getType(), result);
        // Do not generate here; that should be taken care of in the inferContextImpl call above.
        scope =
            (UnsolvedClassOrInterfaceAlternates)
                findExistingAndUpdateFQNs(
                    fullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(
                        constructor.getType()));

        constructorName = constructor.getTypeAsString();
        arguments = constructor.getArguments();

        if (constructor.getTypeArguments().isPresent()) {
          numberOfTypeParams = constructor.getTypeArguments().get().size();
        }
      } else {
        ExplicitConstructorInvocationStmt constructor = (ExplicitConstructorInvocationStmt) node;

        // If it's unresolvable, it's a constructor in the unsolved parent class
        if (!constructor.isThis()) {
          // There can only be one extends in a class
          ClassOrInterfaceType superClass = JavaParserUtil.getSuperClass(node);

          try {
            superClass.resolve();
            // If the type is resolvable, the constructor is too; a type in the constructor is not
            // solvable. Return because we don't need to generate a new constructor.
            return;
          } catch (UnsolvedSymbolException ex) {
            // continue
          }

          inferContextImpl(superClass, result);
          // Do not generate here; that should be taken care of in the inferContextImpl call above.
          scope =
              (UnsolvedClassOrInterfaceAlternates)
                  findExistingAndUpdateFQNs(
                      fullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(superClass));

          constructorName = superClass.getNameAsString();
          arguments = constructor.getArguments();

          if (constructor.getTypeArguments().isPresent()) {
            numberOfTypeParams = constructor.getTypeArguments().get().size();
          }
        } else {
          // We should never reach this case unless the user inputted a bad program (i.e.
          // this(...) constructor call when a definition is not there, or super() without a parent
          // class)
          throw new RuntimeException("Unexpected explicit constructor invocation statement call.");
        }
      }

      if (scope == null) {
        throw new RuntimeException(
            "Scope was not generated in constructor call when it should have been.");
      }

      handleConstructorCall(scope, constructorName, arguments, numberOfTypeParams, result);
    }
    // Method references
    else if (node instanceof MethodReferenceExpr methodRef) {
      handleMethodReferenceExpr(methodRef, result);
    }
    // A lambda expr is not of type Resolvable<?>, but it could be passed into this method
    // when an argument is a lambda.
    else if (node instanceof LambdaExpr lambda) {
      handleLambdaExpr(lambda, result);
    }
    // May be passed into the method if in an annotation.
    else if (node instanceof ClassExpr classExpr) {
      inferContextImpl(classExpr.getType(), result);
    } else if (node instanceof ArrayInitializerExpr arrayInitializerExpr) {
      for (Expression value : arrayInitializerExpr.getValues()) {
        inferContextImpl(value, result);
      }
    }
  }

  /**
   * Helper method for {@link #inferContextImpl(Node, List)}. Handles ClassOrInterfaceType: adds the
   * existing definition to the result if found, or a new definition if one does not already exist.
   *
   * @param type The type to handle
   * @param result The result of inferContext
   */
  private void handleClassOrInterfaceType(
      ClassOrInterfaceType type, List<UnsolvedSymbolAlternates<?>> result) {
    try {
      type.resolve();
      return;
    } catch (UnsolvedSymbolException ex) {
      // Ok to continue
    }
    Set<String> potentialFQNs = fullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(type);

    UnsolvedClassOrInterfaceAlternates generated =
        findExistingAndUpdateFQNsOrCreateNewType(potentialFQNs);

    if (type.getTypeArguments().isPresent()) {
      generated.setNumberOfTypeVariables(type.getTypeArguments().get().size());
    }

    result.add(generated);
  }

  /**
   * Helper method for {@link #inferContextImpl(Node, List)}. Handles annotations: adds the existing
   * definition to the result if found, or a new definition if one does not already exist.
   *
   * @param anno The annotation to handle
   * @param result The result of inferContext
   */
  private void handleAnnotationExpr(AnnotationExpr anno, List<UnsolvedSymbolAlternates<?>> result) {
    try {
      anno.resolve();
      return;
    } catch (UnsolvedSymbolException ex) {
      // Ok to continue
    }
    Set<String> potentialFQNs = fullyQualifiedNameGenerator.getFQNsFromAnnotation(anno);

    UnsolvedClassOrInterfaceAlternates generated =
        findExistingAndUpdateFQNsOrCreateNewType(potentialFQNs);
    generated.setType(UnsolvedClassOrInterfaceType.ANNOTATION);

    result.add(generated);

    // According to JLS 9.6.1
    // (https://docs.oracle.com/javase/specs/jls/se8/html/jls-9.html#jls-9.6.1):
    // * A primitive type
    // * String
    // * Class or an invocation of Class (§4.5)
    // * An enum type
    // * An annotation type
    // * An array type whose component type is one of the preceding types
    // Nested arrays are not valid
    if (anno instanceof SingleMemberAnnotationExpr singleMemberAnnotationExpr) {
      result.add(
          findOrGenerateAnnotationMemberValueMethod(
              singleMemberAnnotationExpr.getMemberValue(), "value", generated, result));
    } else if (anno instanceof NormalAnnotationExpr normalAnnotationExpr) {
      for (MemberValuePair memberValuePair : normalAnnotationExpr.getPairs()) {
        result.add(
            findOrGenerateAnnotationMemberValueMethod(
                memberValuePair.getValue(), memberValuePair.getNameAsString(), generated, result));
      }
    }
  }

  /**
   * Given a member value in an annotation, generate/update a method that represents it in an
   * annotation declaration and return it.
   *
   * @param annotationMemberValue The annotation member value
   * @param name The name of the annotation member value pair
   * @param annotation The annotation to hold this definition
   * @param result The result list
   * @return The generated/found method that represents this member value
   */
  private UnsolvedMethodAlternates findOrGenerateAnnotationMemberValueMethod(
      Expression annotationMemberValue,
      String name,
      UnsolvedClassOrInterfaceAlternates annotation,
      List<UnsolvedSymbolAlternates<?>> result) {
    inferContextImpl(annotationMemberValue, result);

    Expression toLookUpTypeFor = annotationMemberValue;
    boolean isArray = false;
    boolean isEmpty = false;
    if (toLookUpTypeFor.isArrayInitializerExpr()) {
      isArray = true;
      if (toLookUpTypeFor.asArrayInitializerExpr().getValues().isNonEmpty()) {
        toLookUpTypeFor = toLookUpTypeFor.asArrayInitializerExpr().getValues().get(0);
      } else {
        isEmpty = true;
      }
    }

    Set<String> fqns;
    if (isEmpty) {
      // Handle empty arrays (i.e. @Anno({})); we have no way of telling
      // what it actually is
      fqns = Set.of("java.lang.String[]");
    } else {
      fqns = new LinkedHashSet<>();

      Set<String> rawFqns;

      try {
        if (toLookUpTypeFor.isAnnotationExpr()) {
          rawFqns = Set.of(toLookUpTypeFor.asAnnotationExpr().resolve().getQualifiedName());
        } else if (toLookUpTypeFor instanceof FieldAccessExpr fieldAccess
            && JavaParserUtil.looksLikeAConstant(fieldAccess.getNameAsString())) {
          // If it looks like an enum, it probably is
          rawFqns = fullyQualifiedNameGenerator.getFQNsForExpressionType(fieldAccess.getScope());
        } else {
          rawFqns = fullyQualifiedNameGenerator.getFQNsForExpressionType(toLookUpTypeFor);
        }
      } catch (UnsolvedSymbolException ex) {
        rawFqns = fullyQualifiedNameGenerator.getFQNsForExpressionType(toLookUpTypeFor);
      }

      for (String fqn : rawFqns) {
        String erased = JavaParserUtil.erase(fqn);

        // java.lang.Class<...> --> java.lang.Class<?>
        if (!erased.equals(fqn)) {
          fqn = erased + "<?>";
        }

        if (isArray) {
          fqn += "[]";
        }

        fqns.add(fqn);
      }
    }

    MemberType type = getMemberTypeFromFQNs(fqns, false);

    if (type == null) {
      throw new RuntimeException("Annotation member value type must have been generated: " + fqns);
    }

    Set<String> methodFQNs = new LinkedHashSet<>();

    for (String parentFQN : annotation.getFullyQualifiedNames()) {
      methodFQNs.add(parentFQN + "#" + name + "()");
    }

    UnsolvedMethodAlternates gen = (UnsolvedMethodAlternates) findExistingAndUpdateFQNs(methodFQNs);

    if (gen == null) {
      gen = UnsolvedMethodAlternates.create(name, type, List.of(annotation), List.of());
    }
    // If it was created before, the last time could have been an empty array and defaulted to
    // String[]. This will correct it
    // if we discover a type.
    else if (annotationMemberValue.isArrayInitializerExpr()
        && annotationMemberValue.asArrayInitializerExpr().getValues().isNonEmpty()) {
      gen.setReturnType(type);
    }

    return gen;
  }

  /**
   * Helper method for {@link #inferContextImpl(Node, List)}. This method handles cases where
   * FieldAccessExpr could be either a type or a field (when getting the scope of a FieldAccessExpr,
   * it may return another FieldAccessExpr in the form of a class path). Adds the existing
   * definition to the result if found, or a new definition if one does not already exist.
   *
   * @param field The field to handle
   * @param result The result of inferContext
   */
  private void handleFieldAccessExpr(
      FieldAccessExpr field, List<UnsolvedSymbolAlternates<?>> result) {
    // It may be solvable (when passed into this method via scope)
    // In this case, while the declaration may be solvable, the type may not be
    try {
      ResolvedValueDeclaration resolved = field.resolve();

      Type type =
          JavaParserUtil.getTypeFromResolvedValueDeclaration(resolved, fqnsToCompilationUnits);

      if (type != null) {
        inferContextImpl(type, result);
      }

      return;
    } catch (UnsolvedSymbolException ex) {
      // If the declaration is not resolvable, then check to see if it is a
      // known class that has been passed in
      if (JavaParserUtil.isExprTypeResolvable(field)) {
        // This is most likely a class; resolve() only works on field declarations.
        // System.out, for example, would fail to resolve() but calculateResolvedType() would work.
        return;
      }
    }

    // When we have a FieldAccessExpr like a.b.c, the scope a.b is also a FieldAccessExpr
    // We need to handle the case where the scope could be a class, like org.example.MyClass,
    // because resolving the scope of a static field like org.example.MyClass.a would return
    // another FieldAccessExpr, not a ClassOrInterfaceType
    if (JavaParserUtil.isAClassPath(field.toString())) {
      Set<String> potentialFQNs = fullyQualifiedNameGenerator.getFQNsForExpressionType(field);

      UnsolvedClassOrInterfaceAlternates generated =
          findExistingAndUpdateFQNsOrCreateNewType(potentialFQNs);

      result.add(generated);
      return;
    }

    Map<String, Set<String>> potentialScopeFQNs =
        fullyQualifiedNameGenerator.getFQNsForExpressionLocation(field);

    Expression scope = field.getScope();

    // Special case: handle this/super separately since potentialScopeFQNs
    // provides more information than a this/super expression alone in
    // inferContextImpl
    if (scope.isThisExpr() || scope.isSuperExpr()) {
      handleThisOrSuperExpr(potentialScopeFQNs.values());
    } else {
      // Generate everything in the scopes before
      inferContextImpl(scope, result);
    }

    // Could be empty if the field is called on a NameExpr with a union type,
    // but the field is located in a known class.
    if (potentialScopeFQNs.isEmpty()) {
      return;
    }

    Set<String> potentialFQNs = new LinkedHashSet<>();

    for (Set<String> set : potentialScopeFQNs.values()) {
      for (String potentialScopeFQN : set) {
        potentialFQNs.add(potentialScopeFQN + "#" + field.getNameAsString());
      }
    }

    UnsolvedSymbolAlternates<?> alreadyGenerated = findExistingAndUpdateFQNs(potentialFQNs);

    if (!(alreadyGenerated instanceof UnsolvedFieldAlternates)) {
      Set<String> potentialTypeFQNs = fullyQualifiedNameGenerator.getFQNsForExpressionType(field);

      // Since we called inferContextImpl(scope), the field's parents are created
      List<UnsolvedClassOrInterfaceAlternates> potentialParents = new ArrayList<>();
      for (Set<String> set : potentialScopeFQNs.values()) {
        UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(set);

        if (generated == null) {
          throw new RuntimeException("Field scope types are not yet created; FQNs: " + set);
        }
        potentialParents.add((UnsolvedClassOrInterfaceAlternates) generated);
      }

      @SuppressWarnings("unchecked")
      boolean isInAnnotation = field.findAncestor(AnnotationExpr.class).isPresent();

      MemberType type =
          isInAnnotation
              ? new SolvedMemberType("")
              : getOrCreateMemberTypeFromFQNs(potentialTypeFQNs);

      if (isInAnnotation) {
        potentialParents.forEach(parent -> parent.setType(UnsolvedClassOrInterfaceType.ENUM));
      }

      boolean isStatic = JavaParserUtil.getFQNIfStaticMember(field) != null;

      UnsolvedFieldAlternates createdField =
          UnsolvedFieldAlternates.create(
              field.getNameAsString(), type, potentialParents, isStatic, false);

      addNewSymbolToGeneratedSymbolsMap(createdField);
      result.add(createdField);
    }
  }

  /**
   * Helper method for {@link #inferContextImpl(Node, List)}. Adds the existing definition to the
   * result if found, or a new definition if one does not already exist. This method handles cases
   * where NameExpr could be either a type or a field (when getting the scope of a FieldAccessExpr,
   * it may return a NameExpr in the form of a class name, indicated by a capital). Adds the
   * existing definition to the result if found, or a new definition if one does not already exist.
   *
   * @param nameExpr The field/variable to handle
   * @param result The result of inferContext
   */
  private void handleNameExpr(NameExpr nameExpr, List<UnsolvedSymbolAlternates<?>> result) {
    // resolvable (when passed into this method via scope)
    // In this case, while the declaration may be solvable, the type may not be
    try {
      ResolvedValueDeclaration resolved = nameExpr.resolve();

      Type type =
          JavaParserUtil.getTypeFromResolvedValueDeclaration(resolved, fqnsToCompilationUnits);

      if (type != null) {
        inferContextImpl(type, result);
      }

      return;
    } catch (UnsolvedSymbolException ex) {
      // If the declaration is not resolvable, then check to see if it is a
      // known class that has been passed in
      if (JavaParserUtil.isExprTypeResolvable(nameExpr)) {
        // This is most likely a class; resolve() only works on field/variable declarations.
        // System, for example, would fail to resolve() but calculateResolvedType() would work.
        return;
      }
    }

    // class name
    if (JavaParserUtil.isAClassName(nameExpr.getNameAsString())) {
      Set<String> potentialFQNs = fullyQualifiedNameGenerator.getFQNsForExpressionType(nameExpr);

      UnsolvedClassOrInterfaceAlternates generated =
          findExistingAndUpdateFQNsOrCreateNewType(potentialFQNs);

      result.add(generated);
      return;
    }

    Map<String, Set<String>> parentClassFQNs =
        fullyQualifiedNameGenerator.getFQNsForExpressionLocation(nameExpr);
    Set<String> fieldFQNs = new LinkedHashSet<>();

    for (Set<String> set : parentClassFQNs.values()) {
      for (String parentClassFQN : set) {
        fieldFQNs.add(parentClassFQN + "#" + nameExpr.getNameAsString());
      }
    }

    UnsolvedSymbolAlternates<?> generatedField = findExistingAndUpdateFQNs(fieldFQNs);

    if (!(generatedField instanceof UnsolvedFieldAlternates)) {
      // Generate/find the class that will hold the field
      List<UnsolvedClassOrInterfaceAlternates> generatedClasses = new ArrayList<>();

      for (Set<String> fqns : parentClassFQNs.values()) {
        generatedClasses.add(findExistingAndUpdateFQNsOrCreateNewType(fqns));
      }

      // Generate the synthetic type
      Set<String> typeFQNs = fullyQualifiedNameGenerator.getFQNsForExpressionType(nameExpr);

      MemberType type = getOrCreateMemberTypeFromFQNs(typeFQNs);

      // NameExpr and static import must be static and final
      boolean isStaticImport = JavaParserUtil.getFQNIfStaticMember(nameExpr) != null;

      generatedField =
          UnsolvedFieldAlternates.create(
              nameExpr.getNameAsString(), type, generatedClasses, isStaticImport, isStaticImport);

      addNewSymbolToGeneratedSymbolsMap(generatedField);

      result.add(generatedField);
    }
  }

  /**
   * Helper method for {@link #inferContextImpl(Node, List)}. Adds the existing definition to the
   * result if found, or a new definition if one does not already exist.
   *
   * @param methodCall The method call to handle
   * @param result The result of inferContext
   */
  private void handleMethodCallExpr(
      MethodCallExpr methodCall, List<UnsolvedSymbolAlternates<?>> result) {
    try {
      ResolvedMethodDeclaration resolvedMethodDeclaration = methodCall.resolve();

      // If we're here, this was probably passed in as scope
      Node node =
          JavaParserUtil.tryFindAttachedNode(resolvedMethodDeclaration, fqnsToCompilationUnits);

      if (node != null) {
        MethodDeclaration toAst = (MethodDeclaration) node;

        inferContextImpl(toAst.getType(), result);
      }

      return;
    } catch (UnsolvedSymbolException ex) {
      // continue
    }

    // A collection of sets of fqns. Each set represents potentially a different class/interface.
    Collection<Set<String>> potentialScopeFQNs = getMethodLocationFQNs(methodCall);

    // Special case: handle this/super separately since potentialScopeFQNs
    // provides more information than a this/super expression alone in
    // inferContextImpl
    if (methodCall.hasScope()) {
      if (methodCall.getScope().get().isThisExpr() || methodCall.getScope().get().isSuperExpr()) {
        handleThisOrSuperExpr(potentialScopeFQNs);
      } else {
        // Generate everything in the scopes before
        inferContextImpl(methodCall.getScope().get(), result);
      }
    }

    // Could be empty if the method is called on a NameExpr with a union type,
    // but the method is located in a known class.
    if (potentialScopeFQNs.isEmpty()) {
      return;
    }

    Map<Expression, Set<String>> argumentToParameterPotentialFQNs = new HashMap<>();

    Set<String> potentialFQNs =
        getMethodFQNsWithSideEffect(
            methodCall, potentialScopeFQNs, argumentToParameterPotentialFQNs);

    UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(potentialFQNs);

    // Here, try to find if this method call is an argument of a solvable method call
    // If so, we have multiple potential return types to choose from, based on each definition
    Node parent = methodCall.getParentNode().get();
    int paramNum = -1;

    if (parent instanceof NodeWithArguments<?> withArgs) {
      for (int i = 0; i < withArgs.getArguments().size(); i++) {
        if (withArgs.getArgument(i).equals(methodCall)) {
          paramNum = i;
          break;
        }
      }
    }

    List<? extends CallableDeclaration<?>> parentCallableDeclarations = Collections.emptyList();

    // paramNum could still be -1 if methodCall is the scope of another method call,
    // not an argument
    if (paramNum != -1) {
      if (parent instanceof MethodCallExpr parentMethodCall) {
        parentCallableDeclarations =
            JavaParserUtil.tryResolveMethodCallWithUnresolvableArguments(
                parentMethodCall, fqnsToCompilationUnits);
      } else if (parent instanceof ExplicitConstructorInvocationStmt parentConstructorCall) {
        parentCallableDeclarations =
            JavaParserUtil.tryResolveConstructorCallWithUnresolvableArguments(
                parentConstructorCall, fqnsToCompilationUnits);
      } else if (parent instanceof ObjectCreationExpr parentConstructorCall) {
        parentCallableDeclarations =
            JavaParserUtil.tryResolveConstructorCallWithUnresolvableArguments(
                parentConstructorCall, fqnsToCompilationUnits);
      }
    }

    // TODO: see if this is an issue if two different methods have the same parameter type
    Map<MemberType, @Nullable Node> returnTypeToMustPreserveNode = new LinkedHashMap<>();

    for (CallableDeclaration<?> callable : parentCallableDeclarations) {
      Parameter param = callable.getParameter(paramNum);

      Set<String> fqns = fullyQualifiedNameGenerator.getFQNsFromType(param.getType());
      MemberType memberType = getOrCreateMemberTypeFromFQNs(fqns);

      returnTypeToMustPreserveNode.put(memberType, callable);
    }

    UnsolvedMethodAlternates generatedMethod;

    if (generated instanceof UnsolvedMethodAlternates) {
      generatedMethod = (UnsolvedMethodAlternates) generated;

      if (!parentCallableDeclarations.isEmpty()) {
        generatedMethod.updateReturnTypesAndMustPreserveNodes(returnTypeToMustPreserveNode);
      }
    } else {
      Set<String> potentialReturnTypeFQNs =
          fullyQualifiedNameGenerator.getFQNsForExpressionType(methodCall);

      List<UnsolvedClassOrInterfaceAlternates> potentialParents = new ArrayList<>();
      for (Set<String> set : potentialScopeFQNs) {
        UnsolvedSymbolAlternates<?> gen = findExistingAndUpdateFQNs(set);

        if (gen == null) {
          throw new RuntimeException("Method scope types are not yet created");
        }
        potentialParents.add((UnsolvedClassOrInterfaceAlternates) gen);
      }

      List<MemberType> parameters = new ArrayList<>();

      for (Expression argument : methodCall.getArguments()) {
        MemberType paramType;
        try {
          ResolvedType type = argument.calculateResolvedType();
          paramType = new SolvedMemberType(type.describe());
        } catch (UnsolvedSymbolException ex) {
          inferContextImpl(argument, result);

          Set<String> set = argumentToParameterPotentialFQNs.get(argument);

          // This null check is just to satisfy the error checker
          if (set == null) {
            throw new RuntimeException("Expected non-null when this is null");
          }

          paramType = getOrCreateMemberTypeFromFQNs(set);
        }

        parameters.add(paramType);
      }

      if (parentCallableDeclarations.isEmpty()) {
        MemberType returnType = getOrCreateMemberTypeFromFQNs(potentialReturnTypeFQNs);

        generatedMethod =
            UnsolvedMethodAlternates.create(
                methodCall.getNameAsString(), returnType, potentialParents, parameters);
      } else {
        generatedMethod =
            UnsolvedMethodAlternates.create(
                methodCall.getNameAsString(),
                returnTypeToMustPreserveNode,
                potentialParents,
                parameters,
                List.of());
      }

      addNewSymbolToGeneratedSymbolsMap(generatedMethod);

      if (methodCall.getTypeArguments().isPresent()) {
        generatedMethod.setNumberOfTypeVariables(methodCall.getTypeArguments().get().size());
      }
    }

    if (JavaParserUtil.getFQNIfStaticMember(methodCall) != null) {
      generatedMethod.setStatic();
    }

    result.add(generatedMethod);
  }

  /**
   * Utility method to get the FQNs of a method location. If the method's scope is another
   * field/type, this method returns the field type/method return type instead of a synthetic type
   * that could be returned from {@link
   * fullyQualifiedNameGenerator#getFQNsForExpressionLocation(Expression)}.
   *
   * @param methodCall The method call
   * @return A collection of FQN sets, each set representing a different type
   */
  private Collection<Set<String>> getMethodLocationFQNs(MethodCallExpr methodCall) {
    String methodName = methodCall.getNameAsString();

    // Static import
    if (!methodCall.hasScope()) {
      ImportDeclaration importDecl =
          getImportDeclaration(methodName, methodCall.findCompilationUnit().get());

      if (importDecl != null && importDecl.isStatic()) {
        String location = importDecl.getName().getQualifier().get().toString();

        // update or create the type
        findExistingAndUpdateFQNsOrCreateNewType(Set.of(location));
        return List.of(Set.of(location));
      }
    } else if (methodCall.getScope().get() instanceof MethodCallExpr scopeAsMethodCall) {
      // If the scope is a field or a method that's been generated already, we can use that type

      // Get the FQNs of the scope of the scope
      Set<String> potentialScopeScopeFQNs =
          getMethodFQNsWithSideEffect(
              scopeAsMethodCall, getMethodLocationFQNs(scopeAsMethodCall), null);

      UnsolvedMethodAlternates genMethod =
          (UnsolvedMethodAlternates) findExistingAndUpdateFQNs(potentialScopeScopeFQNs);
      if (genMethod != null) {
        return genMethod.getReturnTypes().stream()
            .map(returnTypes -> returnTypes.getFullyQualifiedNames())
            .toList();
      }
    }
    // Handle FieldAccessExpr/NameExpr together here
    else if (methodCall.getScope().get() instanceof FieldAccessExpr
        || methodCall.getScope().get() instanceof NameExpr) {
      Set<String> potentialScopeScopeFQNs = new LinkedHashSet<>();

      String fieldName = ((NodeWithSimpleName<?>) methodCall.getScope().get()).getNameAsString();
      for (Set<String> set :
          fullyQualifiedNameGenerator
              .getFQNsForExpressionLocation(methodCall.getScope().get())
              .values()) {
        for (String potentialScopeFQN : set) {
          potentialScopeScopeFQNs.add(potentialScopeFQN + "#" + fieldName);
        }
      }

      UnsolvedFieldAlternates genField =
          (UnsolvedFieldAlternates) findExistingAndUpdateFQNs(potentialScopeScopeFQNs);
      if (genField != null) {
        return List.of(genField.getType().getFullyQualifiedNames());
      }
    }

    return fullyQualifiedNameGenerator.getFQNsForExpressionLocation(methodCall).values();
  }

  /**
   * Utility method for {@link #handleMethodCallExpr(MethodCallExpr, List)}. Gets method FQNs given
   * a method call expression and a collection of sets of potential FQNs (each set represents a
   * different potential declaring type). argumentToParameterPotentialFQNs is passed in and modified
   * as a side effect (argument mapping to potential type FQNs); if this is null, then there is no
   * side effect.
   *
   * @param methodCall The method call expression
   * @param potentialScopeFQNs Potential scope FQNs
   * @param argumentToParameterPotentialFQNs A map of arguments to their type FQNs; pass in null if
   *     no side effect is desired.
   * @return The set of strings representing the potential FQNs of this method
   */
  private Set<String> getMethodFQNsWithSideEffect(
      MethodCallExpr methodCall,
      Collection<Set<String>> potentialScopeFQNs,
      @Nullable Map<Expression, Set<String>> argumentToParameterPotentialFQNs) {
    List<String> simpleNames = new ArrayList<>();

    for (Expression argument : methodCall.getArguments()) {
      Set<String> fqns = fullyQualifiedNameGenerator.getFQNsForExpressionType(argument);
      String first = fqns.iterator().next();
      String simpleName = JavaParserUtil.getSimpleNameFromQualifiedName(first);
      simpleNames.add(simpleName);
      if (argumentToParameterPotentialFQNs != null) {
        argumentToParameterPotentialFQNs.put(argument, fqns);
      }
    }

    Set<String> potentialFQNs = new LinkedHashSet<>();

    for (Set<String> set : potentialScopeFQNs) {
      for (String potentialScopeFQN : set) {
        potentialFQNs.add(
            potentialScopeFQN
                + "#"
                + methodCall.getNameAsString()
                + "("
                + String.join(", ", simpleNames)
                + ")");
      }
    }

    return potentialFQNs;
  }

  /**
   * Helper method for {@link #inferContextImpl(Node, List)}. Adds the existing definition to the
   * result if found, or a new definition if one does not already exist. Handles both explicit
   * constructor invocation statements (super/this) and constructor calls (new ...()).
   *
   * @param location The location of the constructor
   * @param constructorName The name of the constructor
   * @param arguments The arguments of the constructor call
   * @param numberOfTypeParams The number of type parameters of the constructor only
   * @param result The result of inferContext
   */
  private void handleConstructorCall(
      UnsolvedClassOrInterfaceAlternates location,
      String constructorName,
      List<Expression> arguments,
      int numberOfTypeParams,
      List<UnsolvedSymbolAlternates<?>> result) {
    Map<Expression, Set<String>> argumentToParameterPotentialFQNs = new HashMap<>();
    List<String> simpleNames = new ArrayList<>();

    for (Expression argument : arguments) {
      Set<String> fqns = fullyQualifiedNameGenerator.getFQNsForExpressionType(argument);
      String first = fqns.iterator().next();
      simpleNames.add(JavaParserUtil.getSimpleNameFromQualifiedName(first));
      argumentToParameterPotentialFQNs.put(argument, fqns);
    }

    Set<String> potentialFQNs = new LinkedHashSet<>();

    for (String potentialScopeFQN : location.getFullyQualifiedNames()) {
      potentialFQNs.add(
          potentialScopeFQN + "#" + constructorName + "(" + String.join(", ", simpleNames) + ")");
    }

    UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(potentialFQNs);
    UnsolvedMethodAlternates generatedMethod;

    if (generated instanceof UnsolvedMethodAlternates) {
      generatedMethod = (UnsolvedMethodAlternates) generated;
    } else {
      List<MemberType> parameters = new ArrayList<>();

      for (Expression argument : arguments) {
        MemberType paramType;

        try {
          ResolvedType type = argument.calculateResolvedType();
          paramType = new SolvedMemberType(type.describe());
        } catch (UnsolvedSymbolException ex) {
          inferContextImpl(argument, result);

          Set<String> set = argumentToParameterPotentialFQNs.get(argument);

          // This null check is just to satisfy the error checker
          if (set == null) {
            throw new RuntimeException("Expected non-null when this is null");
          }

          paramType = getOrCreateMemberTypeFromFQNs(set);
        }

        parameters.add(paramType);
      }

      generatedMethod =
          UnsolvedMethodAlternates.create(
              constructorName, new SolvedMemberType(""), List.of(location), parameters);

      addNewSymbolToGeneratedSymbolsMap(generatedMethod);

      generatedMethod.setNumberOfTypeVariables(numberOfTypeParams);

      result.add(generatedMethod);
    }
  }

  /**
   * Helper method for {@link #inferContextImpl(Node, List)}. Generates the method corresponding
   * with the given method reference expression (parameterless void).
   *
   * @param methodRef The method reference expression
   * @param result The result of inferContext
   */
  private void handleMethodReferenceExpr(
      MethodReferenceExpr methodRef, List<UnsolvedSymbolAlternates<?>> result) {
    // It will be the case that a method reference expression is NEVER solvable in this method.
    // Both the method itself and the type of the functional interface must be known in order to
    // resolve, which will never happen here (that would mean a fully solvable method or variable)

    // TODO: it may also be possible to generate alternates based on all the definitions for a
    // method reference expression. For example, obj::foo could refer to foo(int) or foo(int,
    // String). This is the exact same case as the problem we face with constructors/methods with
    // unsolved argument types; fix this code segment once that is also done.

    Map<String, Set<String>> potentialScopeFQNs =
        fullyQualifiedNameGenerator.getFQNsForExpressionLocation(methodRef);
    // A method ref has a scope, so its location is known to be a single type; therefore, it's
    // safe to do this.
    String simpleClassName = potentialScopeFQNs.keySet().iterator().next();

    Set<String> potentialFQNs = new LinkedHashSet<>();

    String methodName = JavaParserUtil.erase(methodRef.getIdentifier());

    boolean isConstructor = methodName.equals("new");

    List<UnsolvedClassOrInterfaceAlternates> scope = new ArrayList<>();

    for (Set<String> set : potentialScopeFQNs.values()) {
      UnsolvedClassOrInterfaceAlternates generated =
          (UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(set);

      if (generated == null) {
        throw new RuntimeException(
            "Method reference scope was not generated when it should have been.");
      }

      scope.add(generated);

      // Unsolvable method ref: default to parameterless
      if (isConstructor) {
        for (String fqn : set) {
          potentialFQNs.add(fqn + "#" + simpleClassName + "()");
        }
      } else {
        for (String fqn : set) {
          potentialFQNs.add(fqn + "#" + methodName + "()");
        }
      }
    }

    UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(potentialFQNs);
    UnsolvedMethodAlternates generatedMethod;

    if (generated instanceof UnsolvedMethodAlternates) {
      generatedMethod = (UnsolvedMethodAlternates) generated;
    } else {
      List<MemberType> parameters = new ArrayList<>();

      generatedMethod =
          UnsolvedMethodAlternates.create(
              isConstructor ? simpleClassName : methodName,
              new SolvedMemberType(isConstructor ? "" : "void"),
              scope,
              parameters);

      if (methodRef.getTypeArguments().isPresent()) {
        generatedMethod.setNumberOfTypeVariables(methodRef.getTypeArguments().get().size());
      }

      addNewSymbolToGeneratedSymbolsMap(generatedMethod);

      result.add(generatedMethod);
    }
  }

  /**
   * Helper method for {@link #inferContextImpl(Node, List)}. Generates a functional interface for
   * the lambda (if a built-in one cannot be used) and adds it to {@code result}.
   *
   * @param lambda The lambda expression
   * @param result The result of inferContext
   */
  private void handleLambdaExpr(LambdaExpr lambda, List<UnsolvedSymbolAlternates<?>> result) {
    boolean isVoid;
    if (lambda.getExpressionBody().isPresent()) {
      Expression body = lambda.getExpressionBody().get();
      Set<String> fqns = fullyQualifiedNameGenerator.getFQNsForExpressionType(body);
      isVoid = fqns.size() == 1 && fqns.iterator().next().equals("void");
    } else {
      isVoid =
          lambda.getBody().asBlockStmt().getStatements().stream()
              .anyMatch(stmt -> stmt instanceof ReturnStmt);
    }

    int arity = lambda.getParameters().size();

    Set<String> unerasedPotentialFQNs =
        fullyQualifiedNameGenerator.getFQNsForExpressionType(lambda);
    Set<String> erasedPotentialFQNs = new LinkedHashSet<>();

    for (String unerased : unerasedPotentialFQNs) {
      if (unerased.startsWith("java.")) {
        // Built-in functional interface can be used; no need for synthetic generation.
        return;
      }

      erasedPotentialFQNs.add(JavaParserUtil.erase(unerased));
    }

    UnsolvedClassOrInterfaceAlternates functionalInterface =
        findExistingAndUpdateFQNsOrCreateNewType(erasedPotentialFQNs);
    functionalInterface.setNumberOfTypeVariables(arity + (isVoid ? 1 : 0));
    result.add(functionalInterface);

    String[] paramArray =
        functionalInterface.getTypeVariablesAsStringWithoutBrackets().split(", ", -1);
    List<MemberType> params = new ArrayList<>();

    // remove the last element of params, because that's the return type, not a parameter
    for (int i = 0; i < params.size() - (isVoid ? 1 : 0); i++) {
      params.add(new SolvedMemberType(paramArray[i]));
    }

    String returnType = isVoid ? "void" : "T" + arity;
    UnsolvedMethodAlternates apply =
        UnsolvedMethodAlternates.create(
            "apply", new SolvedMemberType(returnType), List.of(functionalInterface), params);

    addNewSymbolToGeneratedSymbolsMap(apply);

    result.add(apply);
  }

  /**
   * After checking if an expression's scope is super/this, pass in the value collection of the
   * result of {@link fullyQualifiedNameGenerator#getFQNsForExpressionLocation(Expression)} to this
   * method to ensure all possible types are generated.
   *
   * @param fqnSets The value collection of the result of getFQNsForExpressionLocation, if the scope
   *     is super/this; a collection of FQN sets each representing a different type.
   */
  private void handleThisOrSuperExpr(Collection<Set<String>> fqnSets) {
    for (Set<String> fqnSet : fqnSets) {
      findExistingAndUpdateFQNsOrCreateNewType(fqnSet);
    }
  }

  /**
   * Call this method on each node to gather more information on potential unsolved symbols. Call
   * this method AFTER all unsolved symbols are generated.
   *
   * @param node The node to gather more information from
   * @return An object of type {@link UnsolvedGenerationResult}, usually empty, but the close()
   *     method(s) if first time confirmation of an AutoCloseable, or if the return type is updated
   *     in a method call expression.
   */
  public UnsolvedGenerationResult addInformation(Node node) {
    List<UnsolvedSymbolAlternates<?>> toAdd = new ArrayList<>();
    List<UnsolvedSymbolAlternates<?>> toRemove = new ArrayList<>();

    if (node instanceof ClassOrInterfaceDeclaration decl) {
      for (ClassOrInterfaceType implemented : decl.getImplementedTypes()) {
        UnsolvedClassOrInterfaceAlternates syntheticType =
            (UnsolvedClassOrInterfaceAlternates)
                findExistingAndUpdateFQNs(
                    fullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(implemented));

        if (syntheticType != null) {
          syntheticType.setType(UnsolvedClassOrInterfaceType.INTERFACE);
        }
      }
      if (decl.isInterface()) {
        for (ClassOrInterfaceType implemented : decl.getExtendedTypes()) {
          UnsolvedClassOrInterfaceAlternates syntheticType =
              (UnsolvedClassOrInterfaceAlternates)
                  findExistingAndUpdateFQNs(
                      fullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(implemented));

          if (syntheticType != null) {
            syntheticType.setType(UnsolvedClassOrInterfaceType.INTERFACE);
          }
        }
      }
    } else if (node instanceof MethodDeclaration methodDecl) {
      for (ReferenceType thrownException : methodDecl.getThrownExceptions()) {
        if (!thrownException.isClassOrInterfaceType()) {
          continue;
        }

        UnsolvedClassOrInterfaceAlternates syntheticType =
            (UnsolvedClassOrInterfaceAlternates)
                findExistingAndUpdateFQNs(
                    fullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(
                        thrownException.asClassOrInterfaceType()));

        if (syntheticType != null) {
          syntheticType.extend(new SolvedMemberType("java.lang.Throwable"));
        }
      }
    } else if (node instanceof ThrowStmt throwStmt) {
      UnsolvedClassOrInterfaceAlternates syntheticType =
          (UnsolvedClassOrInterfaceAlternates)
              findExistingAndUpdateFQNs(
                  fullyQualifiedNameGenerator.getFQNsForExpressionType(throwStmt.getExpression()));

      // The type rule dependency map will add method/type declarations to the worklist
      // way before any throw statements will be added. So, if they've already extended the
      // type, we know it is not an uncaught exception.
      if (syntheticType != null && !syntheticType.hasExtends()) {
        syntheticType.extend(new SolvedMemberType("java.lang.RuntimeException"));
      }
    }

    if (node instanceof TryStmt tryStmt) {
      List<@Nullable UnsolvedClassOrInterfaceAlternates> types = new ArrayList<>();
      for (Expression resource : tryStmt.getResources()) {
        // Java 7-8: try (InputStream i = new FileInputStream("file"))
        // Java 9+: try (r)
        // https://javadoc.io/doc/com.github.javaparser/javaparser-core/latest/com/github/javaparser/ast/stmt/TryStmt.html
        if (resource instanceof VariableDeclarationExpr varDeclExpr) {
          // Types of LHS and RHS must extend AutoCloseable

          // Guaranteed to be a class or interface type
          UnsolvedClassOrInterfaceAlternates lhs =
              (UnsolvedClassOrInterfaceAlternates)
                  findExistingAndUpdateFQNs(
                      fullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(
                          varDeclExpr.getElementType().asClassOrInterfaceType()));

          UnsolvedClassOrInterfaceAlternates rhs =
              (UnsolvedClassOrInterfaceAlternates)
                  findExistingAndUpdateFQNs(
                      fullyQualifiedNameGenerator.getFQNsForExpressionType(
                          varDeclExpr.getVariables().get(0).getInitializer().get()));

          types.add(lhs);
          types.add(rhs);
        } else if (resource instanceof NameExpr || resource instanceof FieldAccessExpr) {
          UnsolvedClassOrInterfaceAlternates type =
              (UnsolvedClassOrInterfaceAlternates)
                  findExistingAndUpdateFQNs(
                      fullyQualifiedNameGenerator.getFQNsForExpressionType((Expression) resource));
          types.add(type);
        }
      }

      List<@Nullable UnsolvedClassOrInterfaceAlternates> exceptions = new ArrayList<>();
      for (CatchClause clause : tryStmt.getCatchClauses()) {
        Parameter exception = clause.getParameter();

        if (exception.getType().isClassOrInterfaceType()) {
          UnsolvedClassOrInterfaceAlternates type =
              (UnsolvedClassOrInterfaceAlternates)
                  findExistingAndUpdateFQNs(
                      fullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(
                          exception.getType().asClassOrInterfaceType()));
          exceptions.add(type);
        } else if (exception.getType().isUnionType()) {
          for (ReferenceType refType : exception.getType().asUnionType().getElements()) {
            UnsolvedClassOrInterfaceAlternates type =
                (UnsolvedClassOrInterfaceAlternates)
                    findExistingAndUpdateFQNs(
                        fullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(
                            (ClassOrInterfaceType) refType));
            exceptions.add(type);
          }
        }
      }

      for (UnsolvedClassOrInterfaceAlternates exception : exceptions) {
        MemberType type = new SolvedMemberType("java.lang.Exception");
        if (exception == null || exception.doesExtend(type)) {
          continue;
        }
        exception.extend(type);
      }

      for (UnsolvedClassOrInterfaceAlternates type : types) {
        if (type == null || type.doesImplement("java.lang.AutoCloseable")) {
          continue;
        }

        type.implement("java.lang.AutoCloseable");

        UnsolvedMethodAlternates unsolvedMethodAlternates =
            UnsolvedMethodAlternates.create(
                "close",
                new SolvedMemberType("void"),
                List.of(type),
                List.of(),
                List.of(new SolvedMemberType("java.lang.Exception")));

        addNewSymbolToGeneratedSymbolsMap(unsolvedMethodAlternates);
        toAdd.add(unsolvedMethodAlternates);
      }
    }

    if (node instanceof InstanceOfExpr instanceOf) {
      // If we have x : X and x instanceof Y, then X must be a supertype
      // of Y if X != Y. The JLS says (15.20.2): "If a cast of the RelationalExpression to the
      // ReferenceType would be rejected as a compile-time error, then the instanceof relational
      // expression likewise produces a compile-time error. In such a situation, the result of the
      // instanceof expression could never be true."
      //
      // This method uses this fact to add extends clauses to synthetic classes.
      Type type;
      if (instanceOf.getPattern().isPresent()) {
        PatternExpr patternExpr = instanceOf.getPattern().get();
        type = patternExpr.getType();
      } else {
        type = instanceOf.getType();
      }
      Expression relationalExpr = instanceOf.getExpression();

      MemberType relational =
          getMemberTypeFromFQNs(
              fullyQualifiedNameGenerator.getFQNsForExpressionType(relationalExpr), false);
      UnsolvedClassOrInterfaceAlternates referenceType =
          (UnsolvedClassOrInterfaceAlternates)
              findExistingAndUpdateFQNs(
                  fullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(
                      type.asClassOrInterfaceType()));

      if (relational == null) {
        throw new RuntimeException(
            "Unsolved relational expression when all unsolved symbols should be generated.");
      }
      if (referenceType == null) {
        throw new RuntimeException(
            "Unsolved instanceof type when all unsolved symbols should be generated.");
      }

      // TODO: if extend is filled, put it into implements
      referenceType.extend(relational);
    }

    // This condition checks to see if the return type of a synthetic method definition
    // can be updated by potential child classes.
    // See VoidReturnDoubleTest for an example of why this is necessary
    if (node instanceof MethodCallExpr) {
      // Don't use pattern matching here because it breaks the linter in VS Code
      MethodCallExpr methodCall = (MethodCallExpr) node;
      // Try to resolve where this came from. If we can, we'll look at its declaring type(s)
      // and see if anywhere defines its return type.

      // If the method is resolvable, then we do not need to do this.

      try {
        methodCall.resolve();
        return UnsolvedGenerationResult.EMPTY;
      } catch (UnsolvedSymbolException ex) {
        // continue
      }

      Collection<Set<String>> potentialScopeFQNs = getMethodLocationFQNs(methodCall);

      // Could be empty if the method is called on a NameExpr with a union type,
      // but the method is located in a known class.
      if (potentialScopeFQNs.isEmpty()) {
        return UnsolvedGenerationResult.EMPTY;
      }

      Set<String> potentialFQNs = getMethodFQNsWithSideEffect(methodCall, potentialScopeFQNs, null);

      UnsolvedMethodAlternates alt =
          (UnsolvedMethodAlternates) findExistingAndUpdateFQNs(potentialFQNs);

      if (alt == null) {
        throw new RuntimeException(
            "Unresolvable method is not generated when all unsolved symbols should be.");
      }

      if (alt.getReturnTypes().size() > 1
          || !(alt.getReturnTypes().get(0) instanceof UnsolvedMemberType)) {
        // Return type is not synthetic
        return UnsolvedGenerationResult.EMPTY;
      }

      if (methodCall.hasScope()) {
        Set<ResolvedType> potentialTypes = new LinkedHashSet<>();
        Expression scope = methodCall.getScope().get();
        try {

          ResolvedValueDeclaration resolved;
          if (scope.isFieldAccessExpr()) {
            resolved = scope.asFieldAccessExpr().resolve();
          } else if (scope.isNameExpr()) {
            resolved = scope.asNameExpr().resolve();
          } else {
            // If not a NameExpr or FieldAccessExpr, then we can't gain any more information, since
            // the type of the scope is unsolved.
            return UnsolvedGenerationResult.EMPTY;
          }

          List<VariableDeclarator> variables;

          Node toAst = JavaParserUtil.findAttachedNode(resolved, fqnsToCompilationUnits);

          if (toAst instanceof VariableDeclarationExpr initializer) {
            variables = initializer.getVariables();
          } else if (toAst instanceof FieldDeclaration fieldDecl) {
            variables = fieldDecl.getVariables();
          } else if (toAst instanceof VariableDeclarator varDecl) {
            variables = List.of(varDecl);
          } else {
            variables = List.of();
          }

          for (VariableDeclarator varDecl : variables) {
            if (varDecl.getInitializer().isPresent()
                && JavaParserUtil.isExprTypeResolvable(varDecl.getInitializer().get())) {
              potentialTypes.add(varDecl.getInitializer().get().calculateResolvedType());
            }
          }
        } catch (UnsolvedSymbolException ex) {
          // Initializer could not be resolved, but the field could still be set somewhere
        }

        // Now, find all places where the NameExpr/FieldAccessExpr is set to another type
        TypeDeclaration<?> typeDecl = JavaParserUtil.getEnclosingClassLike(methodCall);

        potentialTypes.addAll(
            typeDecl.findAll(AssignExpr.class).stream()
                .filter(
                    assign ->
                        assign.getOperator() == AssignExpr.Operator.ASSIGN
                            && assign.getTarget().toString().equals(scope.toString())
                            && JavaParserUtil.isExprTypeResolvable(assign.getValue()))
                .map(assign -> assign.calculateResolvedType())
                .toList());

        String methodSignature = potentialFQNs.iterator().next();
        methodSignature =
            methodSignature.substring(potentialFQNs.iterator().next().indexOf('#') + 1);
        for (ResolvedType type : potentialTypes) {
          // Check to see if any of these contain the same method signature; if so, we can
          // update the return type of the current generated one to match it

          // Must be a reference type: if it were not, the method would be solvable, which we
          // checked already
          ResolvedReferenceType refType = type.asReferenceType();

          // The type must also be a user-defined class, not a built-in Java class. This means we
          // cannot get the ResolvedMethodDeclarations from each type declaration since parameter
          // types could be unsolved
          if (refType.getTypeDeclaration().isPresent()) {
            for (ResolvedMethodDeclaration methodDecl :
                refType.getTypeDeclaration().get().getDeclaredMethods()) {
              MethodDeclaration methodDeclAst =
                  (MethodDeclaration)
                      JavaParserUtil.findAttachedNode(methodDecl, fqnsToCompilationUnits);

              String signature =
                  methodDeclAst.getNameAsString()
                      + "("
                      + String.join(
                          ", ",
                          methodDeclAst.getParameters().stream()
                              .map(
                                  param ->
                                      JavaParserUtil.getSimpleNameFromQualifiedName(
                                          param.getTypeAsString()))
                              .toList())
                      + ")";

              if (signature.equals(methodSignature)) {
                UnsolvedClassOrInterfaceAlternates oldReturn =
                    ((UnsolvedMemberType) alt.getReturnTypes().get(0)).getUnsolvedType();
                try {
                  ResolvedType resolvedType = methodDecl.getReturnType();

                  alt.setReturnType(new SolvedMemberType(resolvedType.describe()));

                  toRemove.add(oldReturn);
                  removeSymbolFromGeneratedSymbolsMap(oldReturn);
                } catch (UnsolvedSymbolException ex) {
                  // In this case, remove the old (SomeMethodReturnType) and replace it with a
                  // "better" return type (maybe imported, so we would know a more specific FQN)
                  UnsolvedClassOrInterfaceAlternates generated =
                      findExistingAndUpdateFQNsOrCreateNewType(potentialFQNs);

                  if (!generated.equals(oldReturn)) {
                    toRemove.add(oldReturn);
                    removeSymbolFromGeneratedSymbolsMap(oldReturn);
                    toAdd.add(generated);
                  }
                }

                break;
              }
            }
          }
        }
      }
    }

    return new UnsolvedGenerationResult(toAdd, toRemove);
  }

  /**
   * Returns whether a node needs to undergo post-processing or not; i.e., if {@link
   * #addInformation(Node)} needs to be called on it. This is used in the initial worklist when some
   * unsolved symbols may not be generated yet to defer additional information processing to a time
   * when all unsolved symbols are generated.
   *
   * @param node The node to query about
   * @return Whether {@link #addInformation(Node)} accepts this node
   */
  public boolean needToPostProcess(Node node) {
    return node instanceof ClassOrInterfaceDeclaration
        || node instanceof MethodDeclaration
        || node instanceof TryStmt
        || node instanceof ThrowStmt
        || node instanceof InstanceOfExpr
        || node instanceof MethodCallExpr;
  }

  /**
   * Gets the {@link ImportDeclaration} of the import importing the simple name "importedName".
   * Returns null if no import matching the name is found.
   *
   * @param importedName The imported member; a simple name.
   * @param cu The compilation unit
   * @return The ImportDeclaration representing the import
   */
  private @Nullable ImportDeclaration getImportDeclaration(
      String importedName, CompilationUnit cu) {
    for (ImportDeclaration importDecl : cu.getImports()) {
      if (importDecl.getNameAsString().endsWith("." + importedName)) {
        return importDecl;
      }
    }

    return null;
  }

  /**
   * Same as {@link #findExistingAndUpdateFQNs(Set)} but creates and returns a new type if not
   * found. This only works for type FQNs.
   *
   * @param fqns The set of fqns
   * @return The existing or created definition
   */
  private UnsolvedClassOrInterfaceAlternates findExistingAndUpdateFQNsOrCreateNewType(
      Set<String> fqns) {
    UnsolvedSymbolAlternates<?> existing = findExistingAndUpdateFQNs(fqns);

    if (existing == null) {
      List<UnsolvedClassOrInterfaceAlternates> created =
          UnsolvedClassOrInterfaceAlternates.create(fqns, generatedSymbols);

      for (UnsolvedClassOrInterfaceAlternates c : created) {
        addNewSymbolToGeneratedSymbolsMap(c);
      }
      return created.get(0);
    }

    return (UnsolvedClassOrInterfaceAlternates) existing;
  }

  /**
   * Finds the existing unsolved symbol based on a set of potential FQNs. If none is found, this
   * method returns null. The generatedSymbols map is also modified if the intersection of
   * potentialFQNs and the existing set results in a smaller set of potential FQNs.
   *
   * @param potentialFQNs The set of potential fully-qualified names in the current context.
   * @return The existing symbol, or null if one does not exist yet.
   */
  private @Nullable UnsolvedSymbolAlternates<?> findExistingAndUpdateFQNs(
      Set<String> potentialFQNs) {
    // There is likely only an overlap of FQNs if the two types refer to the same type,
    // but one of these instances may know more information than the other. If it already
    // exists in the generatedSymbols set, we'll keep the most specific set of potential
    // FQNs.

    // For example, if we have in the map an UnsolvedSymbolAlternates with ambiguous mappings
    // of class A: {org.example.A, org.example.ParentClass.A} --> defn, but then we encounter
    // a file with an explicit import org.example.A;, then we know for sure that this type
    // refers to org.example.A, so we'll remove it from the alternates set.

    UnsolvedSymbolAlternates<?> alreadyGenerated = null;
    for (String potentialFQN : potentialFQNs) {
      alreadyGenerated = generatedSymbols.get(potentialFQN);

      if (alreadyGenerated != null) {
        break;
      }
    }

    if (alreadyGenerated != null) {
      UnsolvedClassOrInterfaceAlternates type = null;

      if (alreadyGenerated instanceof UnsolvedClassOrInterfaceAlternates) {
        type = (UnsolvedClassOrInterfaceAlternates) alreadyGenerated;
      } else {
        for (String potentialFQN : potentialFQNs) {
          UnsolvedSymbolAlternates<?> potentialType =
              generatedSymbols.get(potentialFQN.substring(0, potentialFQN.indexOf('#')));

          if (potentialType instanceof UnsolvedClassOrInterfaceAlternates) {
            type = (UnsolvedClassOrInterfaceAlternates) potentialType;
            break;
          }
        }
      }

      if (type == null) {
        throw new RuntimeException(
            "Cannot have generated fields/methods before its type is generated.");
      }

      Set<String> alreadyGeneratedFQNs = alreadyGenerated.getFullyQualifiedNames();

      if (!potentialFQNs.equals(alreadyGeneratedFQNs)) {
        for (String oldFQN : alreadyGeneratedFQNs) {
          generatedSymbols.remove(oldFQN);
        }

        type.updateFullyQualifiedNames(potentialFQNs);

        for (String newFQN : alreadyGenerated.getFullyQualifiedNames()) {
          generatedSymbols.put(newFQN, alreadyGenerated);
        }
      }
    }

    return alreadyGenerated;
  }

  /**
   * Helper method to add a new symbol to {@link #generatedSymbols}.
   *
   * @param newSymbol The new symbol to add
   */
  private void addNewSymbolToGeneratedSymbolsMap(UnsolvedSymbolAlternates<?> newSymbol) {
    for (String potentialFQN : newSymbol.getFullyQualifiedNames()) {
      if (generatedSymbols.containsKey(potentialFQN)) {
        continue;
      }
      generatedSymbols.put(potentialFQN, newSymbol);
    }
  }

  /**
   * Helper method to remove a symbol from {@link #generatedSymbols}.
   *
   * @param symbol The symbol to remove
   */
  private void removeSymbolFromGeneratedSymbolsMap(UnsolvedSymbolAlternates<?> symbol) {
    for (String potentialFQN : symbol.getFullyQualifiedNames()) {
      generatedSymbols.remove(potentialFQN);
    }
  }

  /**
   * Gets the {@code MemberType} from a set of FQNs. If one of the FQNs represents a primitive or
   * built-in java class, then it returns that type. If not, then this method will find an existing
   * generated type, or create it, and return it.
   *
   * @param fqns The set of fully-qualified names
   * @return The member type
   */
  private MemberType getOrCreateMemberTypeFromFQNs(Set<String> fqns) {
    MemberType memberType = getMemberTypeFromFQNs(fqns, true);

    if (memberType == null) {
      throw new RuntimeException("This error is impossible.");
    }

    return memberType;
  }

  /**
   * Gets the {@code MemberType} from a set of FQNs. If one of the FQNs represents a primitive or
   * built-in java class, then it returns that type. If not, then this method will find an existing
   * generated type (or create it, depending on {@code createNew}), and return it.
   *
   * @param fqns The set of fully-qualified names
   * @return The member type
   */
  private @Nullable MemberType getMemberTypeFromFQNs(Set<String> fqns, boolean createNew) {
    for (String fqn : fqns) {
      if (fqnsToCompilationUnits.containsKey(fqn)) return new SolvedMemberType(fqn);

      MemberType type = getMemberTypeIfPrimitiveOrJavaLang(fqn);

      if (type != null) return type;
    }

    // If a set has one element with no dots, it's likely a type variable
    if (fqns.size() == 1 && !fqns.iterator().next().contains(".")) {
      return new SolvedMemberType(fqns.iterator().next());
    }

    UnsolvedClassOrInterfaceAlternates unsolved;

    Set<String> fqnsWithoutArray = new LinkedHashSet<>();

    for (String fqn : fqns) {
      fqnsWithoutArray.add(JavaParserUtil.removeArrayBrackets(fqn));
    }

    if (createNew) {
      unsolved = findExistingAndUpdateFQNsOrCreateNewType(fqnsWithoutArray);
    } else {
      unsolved = (UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(fqnsWithoutArray);
    }

    if (unsolved == null) {
      return null;
    } else {
      return new UnsolvedMemberType(
          unsolved, JavaParserUtil.countNumberOfArrayBrackets(fqns.iterator().next()));
    }
  }

  /**
   * If {@code name} (either a simple name or fully qualified) is primitive, java.lang, or in
   * another java package, then return the MemberType holding it. Else, return null.
   *
   * @param name The name of the type, either simple or fully qualified.
   */
  private @Nullable MemberType getMemberTypeIfPrimitiveOrJavaLang(String name) {
    if (JavaLangUtils.inJdkPackage(JavaParserUtil.removeArrayBrackets(name))
        || JavaLangUtils.isJavaLangOrPrimitiveName(
            JavaParserUtil.getSimpleNameFromQualifiedName(
                JavaParserUtil.removeArrayBrackets(name)))) {
      return new SolvedMemberType(name);
    }
    return null;
  }
}
