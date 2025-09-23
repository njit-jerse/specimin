package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
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
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.utils.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
  /** A map of fully qualified names to their corresponding compilation units. */
  private final Map<String, CompilationUnit> fqnsToCompilationUnits;

  /** Generates fully qualified names for symbols. */
  private final FullyQualifiedNameGenerator fullyQualifiedNameGenerator;

  /**
   * Creates a new UnsolvedSymbolGenerator. Pass in a set of fqns to compilation units for
   * resolution purposes.
   *
   * @param fqnsToCompilationUnits A set of fully-qualified names to compilation units
   */
  public UnsolvedSymbolGenerator(Map<String, CompilationUnit> fqnsToCompilationUnits) {
    this.fqnsToCompilationUnits = fqnsToCompilationUnits;
    fullyQualifiedNameGenerator =
        new FullyQualifiedNameGenerator(fqnsToCompilationUnits, generatedSymbols);
  }

  /**
   * The cache of unsolved symbol definitions. These values need not be unique; the map is provided
   * for simple lookups when adding new symbols. Keys: fully qualified names --> values: unsolved
   * symbol alternates
   */
  private final Map<String, UnsolvedSymbolAlternates<?>> generatedSymbols = new HashMap<>();

  /**
   * Gets all generated symbols.
   *
   * @return The map of fqns to generated symbols.
   */
  public Map<String, UnsolvedSymbolAlternates<?>> getGeneratedSymbols() {
    return generatedSymbols;
  }

  /**
   * Contains all methods that still have null as a parameter type. When encountering a new method
   * signature that replaces each null with a type, remove it from this list and also from
   * generatedSymbols. If one is never found, then replace all instances of null with
   * java.lang.Object.
   */
  private final Set<UnsolvedMethodAlternates> methodsWithNullInSignature = new HashSet<>();

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
    } else if (node instanceof IntersectionType intersection) {
      for (ReferenceType type : intersection.getElements()) {
        inferContextImpl(type, result);
      }
    } else if (node instanceof TypeExpr typeExpr) {
      inferContextImpl(typeExpr.getType(), result);
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
                    fullyQualifiedNameGenerator.getFQNsFromType(constructor.getType()));

        constructorName = constructor.getTypeAsString();
        arguments = constructor.getArguments();

        // While rare, constructors can have type parameters, just like how a method can define
        // its own.
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
                      fullyQualifiedNameGenerator.getFQNsFromType(superClass));

          constructorName = superClass.getNameAsString();
          arguments = constructor.getArguments();
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

      // A constructor call indicates a class
      scope.setType(UnsolvedClassOrInterfaceType.CLASS);

      handleConstructorCall(
          scope, JavaParserUtil.erase(constructorName), arguments, numberOfTypeParams, result);
    } else if (node instanceof MethodDeclaration methodDecl) {
      handleMethodDeclarationWithOverride(methodDecl, result);
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
      ResolvedType resolved = type.resolve();

      if (resolved.isTypeVariable()) {
        TypeParameter typeParam =
            (TypeParameter)
                JavaParserUtil.tryFindAttachedNode(
                    resolved.asTypeParameter(), fqnsToCompilationUnits);

        if (typeParam != null) {
          for (ClassOrInterfaceType bound : typeParam.getTypeBound()) {
            inferContextImpl(bound, result);
          }
        }
      }

      return;
    } catch (UnsolvedSymbolException ex) {
      // Ok to continue
    }
    FullyQualifiedNameSet potentialFQNs = fullyQualifiedNameGenerator.getFQNsFromType(type);

    // ClassOrInterfaceType may be Set<UnknownType>, which would be unresolvable because of
    // UnknownType, but we should not create Set in this case.
    if (doesOverlapWithKnownType(potentialFQNs.erasedFqns())) {
      return;
    }

    UnsolvedClassOrInterfaceAlternates generated =
        findExistingAndUpdateFQNsOrCreateNewType(potentialFQNs.erasedFqns());

    if (generated.getTypeVariables().isEmpty() && type.getTypeArguments().isPresent()) {
      generated.setTypeVariables(type.getTypeArguments().get().size());

      NodeList<Type> typeArgs = type.getTypeArguments().get();
      List<String> typeArgsPreferred = new ArrayList<>(generated.getTypeVariables());

      boolean changed = false;

      for (int i = 0; i < typeArgs.size(); i++) {
        Type typeArg = typeArgs.get(i);

        boolean isTypeParameter = false;
        try {
          isTypeParameter = typeArg.resolve().isTypeVariable();
        } catch (UnsolvedSymbolException ex) {
          // Ok to continue
        }

        if (isTypeParameter) {
          typeArgsPreferred.set(i, typeArg.resolve().asTypeParameter().getName());
          changed = true;
        }
      }

      if (changed) {
        generated.setTypeVariables(typeArgsPreferred);
      }
    }

    result.add(generated);

    // If this type is A, and A is in an extends clause of a non-abstract class, and that class
    // also implements JDK interfaces, and the current declaration has no implementations of must
    // implement methods, we need to generate these methods here.
    if (type.getParentNode().isPresent()
        && type.getParentNode().get() instanceof ClassOrInterfaceDeclaration parent
        && !parent.isInterface()
        && !parent.isAbstract()
        && parent.getExtendedTypes().contains(type)) {
      Set<ResolvedMethodDeclaration> withNoDeclaration =
          JavaParserUtil.getMustImplementMethodsWithNoExistingDeclaration(
              parent, fqnsToCompilationUnits);

      for (ResolvedMethodDeclaration method : withNoDeclaration) {
        Set<String> methodFQNs = new LinkedHashSet<>();

        String signature = method.getName() + "(";
        List<Set<MemberType>> paramTypes = new ArrayList<>();

        for (int i = 0; i < method.getNumberOfParams(); i++) {
          signature +=
              JavaParserUtil.getSimpleNameFromQualifiedName(
                  JavaParserUtil.erase(method.getParam(i).toString()));
          if (i < method.getNumberOfParams() - 1) {
            signature += ", ";
          }

          paramTypes.add(Set.of(new SolvedMemberType(method.getParam(i).describeType())));
        }

        signature += ")";

        for (String parentFQN : generated.getFullyQualifiedNames()) {
          methodFQNs.add(parentFQN + "#" + signature);
        }

        UnsolvedMethodAlternates gen =
            (UnsolvedMethodAlternates) findExistingAndUpdateFQNs(methodFQNs);

        if (gen == null) {
          gen =
              UnsolvedMethodAlternates.create(
                  method.getName(),
                  Set.of(new SolvedMemberType(method.getReturnType().describe())),
                  List.of(generated),
                  paramTypes);
          addNewSymbolToGeneratedSymbolsMap(gen);
          result.add(gen);
        }
      }
    }
  }

  /**
   * Helper method for {@link #inferContextImpl(Node, List)}. Handles annotations: adds the existing
   * definition to the result if found, or a new definition if one does not already exist.
   *
   * @param anno The annotation to handle
   * @param result The result of inferContext
   */
  private void handleAnnotationExpr(AnnotationExpr anno, List<UnsolvedSymbolAlternates<?>> result) {
    // TODO: handle default values when necessary
    try {
      anno.resolve();
      return;
    } catch (UnsolvedSymbolException ex) {
      // Ok to continue
    }
    FullyQualifiedNameSet potentialFQNs = fullyQualifiedNameGenerator.getFQNsFromAnnotation(anno);

    UnsolvedClassOrInterfaceAlternates generated =
        findExistingAndUpdateFQNsOrCreateNewType(potentialFQNs.erasedFqns());
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

    FullyQualifiedNameSet fqns;
    if (isEmpty) {
      // Handle empty arrays (i.e. @Anno({})); we have no way of telling
      // what it actually is
      fqns = new FullyQualifiedNameSet(Set.of("java.lang.String[]"));
    } else {
      FullyQualifiedNameSet rawFqns;

      try {
        if (toLookUpTypeFor.isAnnotationExpr()) {
          rawFqns =
              new FullyQualifiedNameSet(
                  Set.of(toLookUpTypeFor.asAnnotationExpr().resolve().getQualifiedName()));
        } else if (toLookUpTypeFor instanceof FieldAccessExpr fieldAccess
            && JavaParserUtil.looksLikeAConstant(fieldAccess.getNameAsString())) {
          // If it looks like an enum, it probably is
          rawFqns =
              fullyQualifiedNameGenerator
                  .getFQNsForExpressionType(fieldAccess.getScope())
                  .iterator()
                  .next();
        } else {
          rawFqns =
              fullyQualifiedNameGenerator
                  .getFQNsForExpressionType(toLookUpTypeFor)
                  .iterator()
                  .next();
        }
      } catch (UnsolvedSymbolException ex) {
        rawFqns =
            fullyQualifiedNameGenerator.getFQNsForExpressionType(toLookUpTypeFor).iterator().next();
      }

      List<FullyQualifiedNameSet> typeArgs = List.of();
      Set<String> fqnsAsString = new LinkedHashSet<>();
      for (String fqn : rawFqns.erasedFqns()) {
        // java.lang.Class<...> --> java.lang.Class<?>
        if (fqn.equals("java.lang.Class")) {
          typeArgs = List.of(FullyQualifiedNameSet.UNBOUNDED_WILDCARD);
        }

        if (isArray) {
          fqn += "[]";
        }

        fqnsAsString.add(fqn);
      }

      fqns = new FullyQualifiedNameSet(fqnsAsString, typeArgs);
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
      gen = UnsolvedMethodAlternates.create(name, Set.of(type), List.of(annotation), List.of());
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
      for (FullyQualifiedNameSet potentialFQNs :
          fullyQualifiedNameGenerator.getFQNsForExpressionType(field)) {
        UnsolvedClassOrInterfaceAlternates generated =
            findExistingAndUpdateFQNsOrCreateNewType(potentialFQNs.erasedFqns());

        result.add(generated);
      }
      return;
    }

    Collection<Set<String>> potentialScopeFQNs =
        fullyQualifiedNameGenerator.getFQNsForExpressionLocation(field);

    Expression scope = field.getScope();

    // Special case: handle this/super separately since potentialScopeFQNs
    // provides more information than a this/super expression alone in
    // inferContextImpl
    if (scope.isThisExpr() || scope.isSuperExpr()) {
      handleThisOrSuperExpr(potentialScopeFQNs);
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

    for (Set<String> set : potentialScopeFQNs) {
      for (String potentialScopeFQN : set) {
        potentialFQNs.add(potentialScopeFQN + "#" + field.getNameAsString());
      }
    }

    Map<MemberType, CallableDeclaration<?>> typeToMustPreserveNode =
        getTypeToCallableDeclarationFromArgument(field);

    UnsolvedSymbolAlternates<?> alreadyGenerated = findExistingAndUpdateFQNs(potentialFQNs);

    if (!(alreadyGenerated instanceof UnsolvedFieldAlternates)) {
      // Since we called inferContextImpl(scope), the field's parents are created
      List<UnsolvedClassOrInterfaceAlternates> potentialParents = new ArrayList<>();
      for (Set<String> set : potentialScopeFQNs) {
        UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(set);

        if (generated == null) {
          throw new RuntimeException("Field scope types are not yet created; FQNs: " + set);
        }
        potentialParents.add((UnsolvedClassOrInterfaceAlternates) generated);
      }

      @SuppressWarnings("unchecked")
      boolean isInAnnotation = field.findAncestor(AnnotationExpr.class).isPresent();

      if (isInAnnotation) {
        potentialParents.forEach(parent -> parent.setType(UnsolvedClassOrInterfaceType.ENUM));
      }

      boolean isStatic = JavaParserUtil.getFQNIfStaticMember(field) != null;

      UnsolvedFieldAlternates createdField;
      if (typeToMustPreserveNode.isEmpty()) {
        Set<MemberType> types =
            isInAnnotation ? Set.of(new SolvedMemberType("")) : new LinkedHashSet<>();

        if (!isInAnnotation) {
          for (FullyQualifiedNameSet potentialTypeFQNs :
              fullyQualifiedNameGenerator.getFQNsForExpressionType(field)) {
            types.add(getOrCreateMemberTypeFromFQNs(potentialTypeFQNs));
          }
        }

        createdField =
            UnsolvedFieldAlternates.create(
                field.getNameAsString(), types, potentialParents, isStatic, false);
      } else {
        createdField =
            UnsolvedFieldAlternates.create(
                field.getNameAsString(), typeToMustPreserveNode, potentialParents, isStatic, false);
      }

      addNewSymbolToGeneratedSymbolsMap(createdField);
      result.add(createdField);
    } else if (!typeToMustPreserveNode.isEmpty()) {
      ((UnsolvedFieldAlternates) alreadyGenerated)
          .updateFieldTypesAndMustPreserveNodes(typeToMustPreserveNode);
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

        if (type.isUnknownType()) {
          // If unknown type, generate synthetic types for it
          for (FullyQualifiedNameSet fqns :
              fullyQualifiedNameGenerator.getFQNsForExpressionType(nameExpr)) {
            findExistingAndUpdateFQNsOrCreateNewType(fqns.erasedFqns());
          }
        }
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

      FieldDeclaration field =
          (FieldDeclaration)
              JavaParserUtil.tryFindCorrespondingDeclarationInAnonymousClass(nameExpr);
      if (field != null) {
        inferContextImpl(field.getElementType(), result);
        return;
      }
    }

    // class name
    if (JavaParserUtil.isAClassName(nameExpr.getNameAsString())) {
      for (FullyQualifiedNameSet potentialFQNs :
          fullyQualifiedNameGenerator.getFQNsForExpressionType(nameExpr)) {
        UnsolvedClassOrInterfaceAlternates generated =
            findExistingAndUpdateFQNsOrCreateNewType(potentialFQNs.erasedFqns());

        result.add(generated);
      }
      return;
    }

    Collection<Set<String>> parentClassFQNs =
        fullyQualifiedNameGenerator.getFQNsForExpressionLocation(nameExpr);
    Set<String> fieldFQNs = new LinkedHashSet<>();

    for (Set<String> set : parentClassFQNs) {
      for (String parentClassFQN : set) {
        fieldFQNs.add(parentClassFQN + "#" + nameExpr.getNameAsString());
      }
    }

    Map<MemberType, CallableDeclaration<?>> typeToMustPreserveNode =
        getTypeToCallableDeclarationFromArgument(nameExpr);

    UnsolvedSymbolAlternates<?> generatedField = findExistingAndUpdateFQNs(fieldFQNs);

    if (!(generatedField instanceof UnsolvedFieldAlternates)) {
      // Generate/find the class that will hold the field
      List<UnsolvedClassOrInterfaceAlternates> generatedClasses = new ArrayList<>();

      for (Set<String> fqns : parentClassFQNs) {
        generatedClasses.add(findExistingAndUpdateFQNsOrCreateNewType(fqns));
      }

      // NameExpr and static import must be static and final
      boolean isStaticImport = JavaParserUtil.getFQNIfStaticMember(nameExpr) != null;

      if (typeToMustPreserveNode.isEmpty()) {
        Set<MemberType> memberTypes = new LinkedHashSet<>();

        for (FullyQualifiedNameSet typeFQNs :
            fullyQualifiedNameGenerator.getFQNsForExpressionType(nameExpr)) {
          MemberType type = getOrCreateMemberTypeFromFQNs(typeFQNs);

          memberTypes.add(type);
        }

        generatedField =
            UnsolvedFieldAlternates.create(
                nameExpr.getNameAsString(),
                memberTypes,
                generatedClasses,
                isStaticImport,
                isStaticImport);
      } else {
        generatedField =
            UnsolvedFieldAlternates.create(
                nameExpr.getNameAsString(),
                typeToMustPreserveNode,
                generatedClasses,
                isStaticImport,
                isStaticImport);
      }

      addNewSymbolToGeneratedSymbolsMap(generatedField);

      result.add(generatedField);
    } else if (!typeToMustPreserveNode.isEmpty()) {
      ((UnsolvedFieldAlternates) generatedField)
          .updateFieldTypesAndMustPreserveNodes(typeToMustPreserveNode);
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

      // If we're here, this was probably passed in as scope/argument
      Node node =
          JavaParserUtil.tryFindAttachedNode(resolvedMethodDeclaration, fqnsToCompilationUnits);

      if (node != null) {
        MethodDeclaration toAst = (MethodDeclaration) node;

        inferContextImpl(toAst.getType(), result);
      }

      return;
    } catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
      // continue
    }

    List<? extends CallableDeclaration<?>> definitions =
        JavaParserUtil.tryResolveNodeWithUnresolvableArguments(methodCall, fqnsToCompilationUnits);

    if (!definitions.isEmpty()
        && methodCall.getArguments().stream()
            .allMatch(
                arg ->
                    JavaParserUtil.isExprTypeResolvable(arg)
                        || JavaParserUtil.isExprDefinitionResolvable(arg))) {
      // Special case: method declaration is findable, arguments are all solvable, but a parameter
      // type is not. In this case, the type of the parameters are unsolved, and should be preserved
      // if the parameter type ever ends up becoming used (which it will, after addInformation is
      // done).
      for (CallableDeclaration<?> callable : definitions) {
        for (Parameter param : callable.getParameters()) {
          List<UnsolvedSymbolAlternates<?>> generated = inferContext(param.getType());
          // Find the generated param type, if any
          for (UnsolvedSymbolAlternates<?> symbol : generated) {
            if (symbol instanceof UnsolvedClassOrInterfaceAlternates type) {
              if (type.getClassName().equals(JavaParserUtil.erase(param.getTypeAsString()))) {
                for (UnsolvedClassOrInterface alt : type.getAlternates()) {
                  alt.addMustPreserveNode(callable);
                }
                break;
              }
            }
          }
        }
      }
    }

    // A collection of sets of fqns. Each set represents potentially a different class/interface.
    Collection<Set<String>> potentialScopeFQNs =
        fullyQualifiedNameGenerator.getFQNsForExpressionLocation(methodCall);

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
    // If there are no methods that match this in the type or its ancestors, we need to generate it.
    else if (definitions.isEmpty()) {
      handleThisOrSuperExpr(potentialScopeFQNs);
    }

    // Could be empty if the method is called on a NameExpr with a union type,
    // but the method is located in a known class.

    // potentialScopeFQNs may also be size 1 if it is unresolvable due to its location in a
    // lambda body. The second part of the condition checks for this edge case, where the method
    // may be known.
    if (potentialScopeFQNs.isEmpty()
        || (potentialScopeFQNs.size() == 1
            && doesOverlapWithKnownType(potentialScopeFQNs.iterator().next()))) {
      return;
    }

    Map<Expression, Set<FullyQualifiedNameSet>> argumentToParameterPotentialFQNs = new HashMap<>();

    Set<String> potentialFQNs =
        fullyQualifiedNameGenerator.generateMethodFQNsWithSideEffect(
            methodCall, potentialScopeFQNs, argumentToParameterPotentialFQNs, true);
    boolean hasNullInSignature =
        argumentToParameterPotentialFQNs.keySet().stream().anyMatch(Expression::isNullLiteralExpr);

    if (hasNullInSignature) {
      Set<String> scopesFlattened =
          potentialScopeFQNs.stream().flatMap(Set::stream).collect(Collectors.toSet());

      // If we see null, try to find an existing generated method which has an object instead
      for (String fqn : generatedSymbols.keySet()) {
        UnsolvedSymbolAlternates<?> gen = generatedSymbols.get(fqn);
        if (gen instanceof UnsolvedMethodAlternates) {
          String qualifiedMethodName = fqn.substring(0, fqn.indexOf('('));
          String methodName =
              qualifiedMethodName.substring(qualifiedMethodName.lastIndexOf('#') + 1);
          String className = qualifiedMethodName.substring(0, qualifiedMethodName.lastIndexOf('#'));

          if (!methodName.equals(methodCall.getNameAsString())
              || !scopesFlattened.contains(className)) {
            continue;
          }

          String[] parameterList =
              fqn.substring(fqn.indexOf('(') + 1).replace(")", "").split(",\\s*", -1);

          if (parameterList.length != methodCall.getArguments().size()) {
            continue;
          }

          boolean valid = true;
          for (int i = 0; i < parameterList.length; i++) {
            String parameter = parameterList[i];
            if (parameter.trim().equals("null")) {
              valid = false;
              break;
            }

            if (methodCall.getArgument(i).isNullLiteralExpr()
                && JavaLangUtils.isPrimitive(parameter)) {
              valid = false;
              break;
            }

            Set<FullyQualifiedNameSet> fqns =
                argumentToParameterPotentialFQNs.get(methodCall.getArgument(i));
            if (fqns == null
                || !fqns.stream()
                    .anyMatch(
                        fqnSet -> fqnSet.erasedFqns().contains(JavaParserUtil.erase(parameter)))) {
              valid = false;
              break;
            }
          }

          if (valid) {
            // If any exists, we don't have to create any method
            return;
          }
        }
      }
    }

    UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(potentialFQNs);

    // TODO: see if this is an issue if two different methods have the same parameter type
    Map<MemberType, CallableDeclaration<?>> returnTypeToMustPreserveNode =
        getTypeToCallableDeclarationFromArgument(methodCall);

    UnsolvedMethodAlternates generatedMethod;

    if (generated instanceof UnsolvedMethodAlternates) {
      generatedMethod = (UnsolvedMethodAlternates) generated;

      if (!returnTypeToMustPreserveNode.isEmpty()) {
        generatedMethod.updateReturnTypesAndMustPreserveNodes(returnTypeToMustPreserveNode);
      }
    } else {
      List<UnsolvedClassOrInterfaceAlternates> potentialParents = new ArrayList<>();
      for (Set<String> set : potentialScopeFQNs) {
        if (doesOverlapWithKnownType(set)) {
          return;
        }

        UnsolvedSymbolAlternates<?> gen = findExistingAndUpdateFQNs(set);

        if (gen == null) {
          throw new RuntimeException(
              "Method scope types are not yet created: " + methodCall + " with scope " + set);
        }
        potentialParents.add((UnsolvedClassOrInterfaceAlternates) gen);
      }

      for (Expression argument : methodCall.getArguments()) {
        inferContextImpl(argument, result);
      }

      List<Map<MemberType, @Nullable Node>> parametersToMustPreserve =
          generateParameterToMustPreserveMap(
              methodCall.getArguments(), argumentToParameterPotentialFQNs);

      if (returnTypeToMustPreserveNode.isEmpty()) {
        MethodDeclaration declarationInThisTypeWithSameSignature =
            JavaParserUtil.tryFindMethodDeclarationWithSameSignatureFromThisType(
                methodCall, fqnsToCompilationUnits);

        Set<MemberType> returnTypes = new LinkedHashSet<>();
        if (declarationInThisTypeWithSameSignature != null) {
          returnTypes.add(
              getOrCreateMemberTypeFromFQNs(
                  fullyQualifiedNameGenerator.getFQNsFromType(
                      declarationInThisTypeWithSameSignature.getType())));
        } else {
          for (FullyQualifiedNameSet fqns :
              fullyQualifiedNameGenerator.getFQNsForExpressionType(methodCall)) {
            returnTypes.add(getOrCreateMemberTypeFromFQNs(fqns));
          }
        }

        generatedMethod =
            UnsolvedMethodAlternates.createWithPreservation(
                methodCall.getNameAsString(),
                returnTypes,
                potentialParents,
                parametersToMustPreserve,
                List.of());
      } else {
        generatedMethod =
            UnsolvedMethodAlternates.createWithPreservation(
                methodCall.getNameAsString(),
                returnTypeToMustPreserveNode,
                potentialParents,
                parametersToMustPreserve,
                List.of());
      }

      if (hasNullInSignature) {
        methodsWithNullInSignature.add(generatedMethod);
      } else if (!methodsWithNullInSignature.isEmpty()) {
        Set<String> scopesFlattened =
            potentialScopeFQNs.stream().flatMap(Set::stream).collect(Collectors.toSet());

        UnsolvedMethodAlternates toRemove = null;
        for (UnsolvedMethodAlternates method : methodsWithNullInSignature) {
          for (String fqn : method.getFullyQualifiedNames()) {
            String qualifiedMethodName = fqn.substring(0, fqn.indexOf('('));
            String methodName =
                qualifiedMethodName.substring(qualifiedMethodName.lastIndexOf('#') + 1);
            String className =
                qualifiedMethodName.substring(0, qualifiedMethodName.lastIndexOf('#'));

            if (!methodName.equals(methodCall.getNameAsString())
                || !scopesFlattened.contains(className)) {
              continue;
            }

            String[] parameterList =
                fqn.substring(fqn.indexOf('(') + 1).replace(")", "").split(",\\s*", -1);

            if (parameterList.length != methodCall.getArguments().size()) {
              continue;
            }

            boolean valid = true;
            for (int i = 0; i < parameterList.length; i++) {
              String parameter = parameterList[i];
              Expression arg = methodCall.getArgument(i);

              Set<FullyQualifiedNameSet> fqns = argumentToParameterPotentialFQNs.get(arg);

              if (fqns == null) {
                valid = false;
                break;
              }

              Set<String> argumentFQNsFlattened =
                  fqns.stream()
                      .flatMap(fqnSet -> fqnSet.erasedFqns().stream())
                      .collect(Collectors.toSet());

              if (parameter.equals("null")) {
                if (argumentFQNsFlattened.stream().anyMatch(JavaLangUtils::isPrimitive)) {
                  valid = false;
                  break;
                }
                continue;
              }

              if (!argumentFQNsFlattened.contains(JavaParserUtil.erase(parameter))) {
                valid = false;
                break;
              }
            }

            if (valid) {
              toRemove = method;
              break;
            }
          }

          if (toRemove != null) {
            break;
          }
        }

        if (toRemove != null) {
          methodsWithNullInSignature.remove(toRemove);
          removeSymbolFromGeneratedSymbolsMap(toRemove);
        }
      }
      addNewSymbolToGeneratedSymbolsMap(generatedMethod);

      if (methodCall.getTypeArguments().isPresent()) {
        generatedMethod.setNumberOfTypeVariables(methodCall.getTypeArguments().get().size());
      }
    }

    if (JavaParserUtil.getFQNIfStaticMember(methodCall) != null) {
      generatedMethod.setStatic();
    }

    if (!hasNullInSignature) {
      // Never add a method with a null parameter
      result.add(generatedMethod);
    }
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
    Map<Expression, Set<FullyQualifiedNameSet>> argumentToParameterPotentialFQNs = new HashMap<>();
    List<Set<String>> simpleNames = new ArrayList<>();

    for (Expression argument : arguments) {
      Set<FullyQualifiedNameSet> fqns =
          fullyQualifiedNameGenerator.getFQNsForExpressionType(argument);
      Set<String> simpleNamesForThisArgumentType = new LinkedHashSet<>();
      for (FullyQualifiedNameSet fqnSet : fqns) {
        String first = fqnSet.erasedFqns().iterator().next();
        simpleNamesForThisArgumentType.add(JavaParserUtil.getSimpleNameFromQualifiedName(first));
      }
      simpleNames.add(simpleNamesForThisArgumentType);
      argumentToParameterPotentialFQNs.put(argument, fqns);
    }

    Set<String> potentialFQNs = new LinkedHashSet<>();

    for (List<String> simpleNamesCombo : JavaParserUtil.generateAllCombinations(simpleNames)) {
      for (String potentialScopeFQN : location.getFullyQualifiedNames()) {
        potentialFQNs.add(
            potentialScopeFQN
                + "#"
                + constructorName
                + "("
                + String.join(", ", simpleNamesCombo)
                + ")");
      }
    }

    UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(potentialFQNs);
    UnsolvedMethodAlternates generatedMethod;

    if (generated instanceof UnsolvedMethodAlternates) {
      generatedMethod = (UnsolvedMethodAlternates) generated;
    } else {
      for (Expression argument : arguments) {
        inferContextImpl(argument, result);
      }

      List<Map<MemberType, @Nullable Node>> parametersToMustPreserve =
          generateParameterToMustPreserveMap(arguments, argumentToParameterPotentialFQNs);

      generatedMethod =
          UnsolvedMethodAlternates.createWithPreservation(
              constructorName,
              Set.of(new SolvedMemberType("")),
              List.of(location),
              parametersToMustPreserve,
              List.of());

      addNewSymbolToGeneratedSymbolsMap(generatedMethod);

      generatedMethod.setNumberOfTypeVariables(numberOfTypeParams);

      result.add(generatedMethod);
    }
  }

  /**
   * Given a list of argument expressions (from a method call, constructor call) and a map of
   * arguments to potential FQN sets, return a list of maps, each representing mutually exclusive
   * parameter types to nodes that must be preserved if that parameter type is used.
   *
   * @param args The collection of argument expressions
   * @param argumentToParameterPotentialFQNs A map from each argument expression to its potential
   *     fully qualified name sets
   * @return A list of maps, each representing mutually exclusive parameter types to nodes that must
   *     be preserved
   */
  private List<Map<MemberType, @Nullable Node>> generateParameterToMustPreserveMap(
      Collection<Expression> args,
      Map<Expression, Set<FullyQualifiedNameSet>> argumentToParameterPotentialFQNs) {
    List<Map<MemberType, @Nullable Node>> parametersToMustPreserve = new ArrayList<>();

    for (Expression argument : args) {
      Set<FullyQualifiedNameSet> potentialParameterTypes =
          argumentToParameterPotentialFQNs.get(argument);

      // This null check is just to satisfy the error checker
      if (potentialParameterTypes == null) {
        throw new RuntimeException("Expected non-null when this is null");
      }

      Map<FullyQualifiedNameSet, Node> potentialParameterToMustPreserveNode = new HashMap<>();
      if (argument.isMethodReferenceExpr()) {
        List<? extends ResolvedMethodLikeDeclaration> resolved =
            JavaParserUtil.getMethodDeclarationsFromMethodRef(argument.asMethodReferenceExpr());

        for (ResolvedMethodLikeDeclaration method : resolved) {
          CallableDeclaration<?> ast =
              (CallableDeclaration<?>)
                  JavaParserUtil.tryFindAttachedNode(method, fqnsToCompilationUnits);

          if (ast == null) {
            continue;
          }

          potentialParameterToMustPreserveNode.put(
              fullyQualifiedNameGenerator.getFunctionalInterfaceForResolvedMethod(
                  argument.asMethodReferenceExpr(), method),
              ast);
        }
      }

      for (FullyQualifiedNameSet potentialParameterType : potentialParameterTypes) {
        parametersToMustPreserve.add(
            Collections.singletonMap(
                getOrCreateMemberTypeFromFQNs(potentialParameterType),
                potentialParameterToMustPreserveNode.get(potentialParameterType)));
      }
    }

    return parametersToMustPreserve;
  }

  /**
   * Helper method for {@link #inferContextImpl(Node, List)}. Given an existing method declaration
   * with {@code @Override}, generates a synthetic method with the same parameter and return types
   * with potential declaring types in all unsolvable ancestors.
   *
   * @param methodDecl The method declaration to process
   * @param result The result list to add generated symbols to
   */
  private void handleMethodDeclarationWithOverride(
      MethodDeclaration methodDecl, List<UnsolvedSymbolAlternates<?>> result) {
    Collection<Set<String>> potentialScopeFQNs;
    if (methodDecl.getParentNode().orElse(null) instanceof ObjectCreationExpr anonClass) {
      try {
        ResolvedType resolvedType = anonClass.getType().resolve();

        TypeDeclaration<?> parentClass =
            JavaParserUtil.getTypeFromQualifiedName(
                resolvedType.describe(), fqnsToCompilationUnits);

        if (parentClass == null) {
          return;
        }

        potentialScopeFQNs =
            fullyQualifiedNameGenerator
                .getFQNsOfAllUnresolvableParents(parentClass, methodDecl)
                .values();
      } catch (UnsolvedSymbolException ex) {
        potentialScopeFQNs =
            Set.of(fullyQualifiedNameGenerator.getFQNsFromType(anonClass.getType()).erasedFqns());
      }
    } else {
      potentialScopeFQNs =
          fullyQualifiedNameGenerator
              .getFQNsOfAllUnresolvableParents(
                  JavaParserUtil.getEnclosingClassLike(methodDecl), methodDecl)
              .values();
    }

    if (potentialScopeFQNs.isEmpty()) {
      // If there are no potential scope FQNs, then this method is likely an override of a method
      // in an existing class or JDK interface
      return;
    }

    String simpleSignature = methodDecl.getNameAsString() + "(";

    for (Parameter param : methodDecl.getParameters()) {
      simpleSignature +=
          JavaParserUtil.getSimpleNameFromQualifiedName(
              JavaParserUtil.erase(param.getTypeAsString()));
    }

    simpleSignature += ")";

    Set<String> potentialMethodFQNs = new LinkedHashSet<>();
    for (Set<String> set : potentialScopeFQNs) {
      for (String fqn : set) {
        potentialMethodFQNs.add(fqn + "#" + simpleSignature);
      }
    }

    UnsolvedMethodAlternates generated =
        (UnsolvedMethodAlternates) findExistingAndUpdateFQNs(potentialMethodFQNs);

    if (generated != null) {
      return;
    }

    List<MemberType> parameters = new ArrayList<>();
    for (Parameter param : methodDecl.getParameters()) {
      MemberType paramType =
          getOrCreateMemberTypeFromFQNs(
              fullyQualifiedNameGenerator.getFQNsFromType(param.getType()));
      parameters.add(paramType);
    }

    List<UnsolvedClassOrInterfaceAlternates> potentialDeclaringTypes = new ArrayList<>();

    for (Set<String> fqns : potentialScopeFQNs) {
      potentialDeclaringTypes.add(findExistingAndUpdateFQNsOrCreateNewType(fqns));
    }

    MemberType returnType =
        getOrCreateMemberTypeFromFQNs(
            fullyQualifiedNameGenerator.getFQNsFromType(methodDecl.getType()));

    List<MemberType> exceptions = new ArrayList<>();
    for (ReferenceType exception : methodDecl.getThrownExceptions()) {
      MemberType exceptionType =
          getOrCreateMemberTypeFromFQNs(fullyQualifiedNameGenerator.getFQNsFromType(exception));
      exceptions.add(exceptionType);
    }

    AccessSpecifier specifier = methodDecl.getAccessSpecifier();
    String accessModifier =
        switch (specifier) {
          case PUBLIC -> "public";
          case PROTECTED -> "protected";
          case PRIVATE -> throw new RuntimeException("Cannot override with a private method.");
          case NONE -> "";
        };

    generated =
        UnsolvedMethodAlternates.create(
            methodDecl.getNameAsString(),
            Set.of(returnType),
            potentialDeclaringTypes,
            parameters.stream().map(p -> Set.of(p)).toList(),
            exceptions,
            accessModifier);

    addNewSymbolToGeneratedSymbolsMap(generated);
    result.add(generated);
  }

  /**
   * Helper method for {@link #inferContextImpl(Node, List)}. Generates the method corresponding
   * with the given method reference expression (parameterless void). If the method reference
   * matches a method in java.lang.Object, no new method is generated. Likewise, if a method with
   * the same qualified name (not signature) is already generated, no new method is created. In
   * other cases, a new, parameterless void method is generated and added to the result.
   *
   * @param methodRef The method reference expression
   * @param result The result of inferContext
   */
  private void handleMethodReferenceExpr(
      MethodReferenceExpr methodRef, List<UnsolvedSymbolAlternates<?>> result) {
    if (JavaLangUtils.getJavaLangObjectMethods().keySet().stream()
        .anyMatch(k -> k.startsWith(methodRef.getIdentifier()))) {
      // If the method reference matches a method in java.lang.Object, we can use that method
      // directly without generating a new one.
      return;
    }

    boolean needToGenerateMethod =
        fullyQualifiedNameGenerator.getExpressionTypesIfRepresentsGenerated(methodRef) == null
            && JavaParserUtil.getMethodDeclarationsFromMethodRef(methodRef).isEmpty();

    String methodName = JavaParserUtil.erase(methodRef.getIdentifier());
    boolean isConstructor = false;

    if (methodName.equals("new")) {
      methodName =
          JavaParserUtil.getSimpleNameFromQualifiedName(
              JavaParserUtil.erase(methodRef.getScope().toString()));
      isConstructor = true;
    }

    List<UnsolvedClassOrInterfaceAlternates> scope = new ArrayList<>();
    Collection<Set<String>> potentialScopeFQNs;
    Set<String> scopeFQNsFlattened;

    if (needToGenerateMethod) {
      inferContextImpl(methodRef.getScope(), result);

      potentialScopeFQNs = fullyQualifiedNameGenerator.getFQNsForExpressionLocation(methodRef);
      scopeFQNsFlattened =
          potentialScopeFQNs.stream().flatMap(Set::stream).collect(Collectors.toSet());

      for (Set<String> set : potentialScopeFQNs) {
        UnsolvedClassOrInterfaceAlternates classOrInterface =
            (UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(set);

        if (classOrInterface == null) {
          throw new RuntimeException(
              "Type is not generated for method reference scope: "
                  + methodRef
                  + " with FQNs "
                  + set);
        }

        scope.add(classOrInterface);
      }
    } else {
      potentialScopeFQNs = Set.of();
      scopeFQNsFlattened = Set.of();
    }

    for (FullyQualifiedNameSet functionalInterface :
        fullyQualifiedNameGenerator.getFQNsForExpressionType(methodRef)) {
      FullyQualifiedNameSet normalized =
          FunctionalInterfaceHelper.convertToNormalFunctionalInterface(functionalInterface);

      List<FullyQualifiedNameSet> parameters = new ArrayList<>(normalized.typeArguments());
      FullyQualifiedNameSet returnTypeFromTypeArgs;

      String funcIntName =
          JavaParserUtil.getSimpleNameFromQualifiedName(normalized.erasedFqns().iterator().next());

      if (funcIntName.contains("SyntheticConsumer")) {
        returnTypeFromTypeArgs = null;
      } else if (funcIntName.contains("SyntheticFunction")) {
        returnTypeFromTypeArgs =
            normalized.typeArguments().get(normalized.typeArguments().size() - 1);
      } else {
        returnTypeFromTypeArgs =
            FunctionalInterfaceHelper.getReturnTypeFromNormalizedFunctionalInterface(normalized);
      }

      MemberType returnType;

      boolean isVoid = false;
      if (isConstructor) {
        if (returnTypeFromTypeArgs != null) {
          parameters.remove(parameters.size() - 1);
        }

        returnType = new SolvedMemberType("");
      } else {
        if (returnTypeFromTypeArgs != null) {
          parameters.remove(parameters.size() - 1);

          // Get rid of the wildcard
          FullyQualifiedNameSet unwildcarded = returnTypeFromTypeArgs;

          if (returnTypeFromTypeArgs.wildcard() != null) {
            if (returnTypeFromTypeArgs.equals(FullyQualifiedNameSet.UNBOUNDED_WILDCARD)) {
              unwildcarded = new FullyQualifiedNameSet("java.lang.Object");
            } else {
              unwildcarded =
                  new FullyQualifiedNameSet(
                      returnTypeFromTypeArgs.erasedFqns(), returnTypeFromTypeArgs.typeArguments());
            }
          }

          returnType = getOrCreateMemberTypeFromFQNs(unwildcarded);
        } else {
          isVoid = true;
          returnType = new SolvedMemberType("void");
        }
      }

      boolean isStatic = false;

      if (methodRef.getScope().isTypeExpr()) {
        if (parameters.isEmpty()) {
          isStatic = true;
        } else {
          FullyQualifiedNameSet param1 = parameters.get(0);

          if (param1.erasedFqns().stream().anyMatch(scopeFQNsFlattened::contains)) {
            parameters.remove(0);
          } else {
            isStatic = true;
          }
        }
      }

      result.addAll(
          generateFunctionalInterface(normalized.erasedFqns(), parameters.size(), isVoid));

      if (!needToGenerateMethod) {
        continue;
      }

      List<String> simpleNames = new ArrayList<>();
      List<Set<MemberType>> parametersAsMemberType = new ArrayList<>();

      for (FullyQualifiedNameSet param : parameters) {
        String simpleName =
            JavaParserUtil.getSimpleNameFromQualifiedName(param.erasedFqns().iterator().next());
        simpleNames.add(simpleName);
        parametersAsMemberType.add(Set.of(getOrCreateMemberTypeFromFQNs(param)));
      }

      Set<String> potentialFQNs = new LinkedHashSet<>();

      for (Set<String> set : potentialScopeFQNs) {
        for (String potentialScopeFQN : set) {
          potentialFQNs.add(
              potentialScopeFQN + "#" + methodName + "(" + String.join(", ", simpleNames) + ")");
        }
      }

      UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(potentialFQNs);

      if (generated == null) {
        UnsolvedMethodAlternates generatedMethod =
            UnsolvedMethodAlternates.create(
                methodName, Set.of(returnType), scope, parametersAsMemberType);

        if (isStatic) {
          generatedMethod.setStatic();
        }

        if (methodRef.getTypeArguments().isPresent()) {
          generatedMethod.setNumberOfTypeVariables(methodRef.getTypeArguments().get().size());
        }

        addNewSymbolToGeneratedSymbolsMap(generatedMethod);

        result.add(generatedMethod);
      }
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
      Set<FullyQualifiedNameSet> fqns = fullyQualifiedNameGenerator.getFQNsForExpressionType(body);
      isVoid =
          fqns.size() == 1
              && fqns.iterator().next().erasedFqns().size() == 1
              && fqns.iterator().next().erasedFqns().iterator().next().equals("void");
    } else {
      isVoid =
          !lambda.getBody().asBlockStmt().getStatements().stream()
              .anyMatch(stmt -> stmt instanceof ReturnStmt);
    }

    int arity = lambda.getParameters().size();

    // Lambdas will always only have one type
    FullyQualifiedNameSet potentialFQNs =
        fullyQualifiedNameGenerator.getFQNsForExpressionType(lambda).iterator().next();

    for (String unerased : potentialFQNs.erasedFqns()) {
      if (unerased.startsWith("java.")) {
        // Built-in functional interface can be used; no need for synthetic generation.
        return;
      }
    }

    result.addAll(generateFunctionalInterface(potentialFQNs.erasedFqns(), arity, isVoid));
  }

  /**
   * Creates a new functional interface and its method. Returns generated symbols as a list; if none
   * needed to be generated, then returns an empty list.
   *
   * @param fqns The set of erased fqns representing this functional interface
   * @param arity The number of parameters
   * @param isVoid Whether the functional interface's method returns void
   * @return A list of generated symbols, or an empty list if none were generated
   */
  private List<UnsolvedSymbolAlternates<?>> generateFunctionalInterface(
      Set<String> fqns, int arity, boolean isVoid) {
    if (doesOverlapWithKnownType(fqns)) {
      return Collections.emptyList();
    }

    UnsolvedClassOrInterfaceAlternates functionalInterface =
        findExistingAndUpdateFQNsOrCreateNewType(fqns);
    functionalInterface.setTypeVariables(arity + (isVoid ? 0 : 1));
    functionalInterface.setType(UnsolvedClassOrInterfaceType.INTERFACE);
    functionalInterface.addAnnotation("@FunctionalInterface");

    List<String> paramList = functionalInterface.getTypeVariables();
    List<Set<MemberType>> params = new ArrayList<>();

    // remove the last element of params, because that's the return type, not a parameter
    for (int i = 0; i < paramList.size() - (isVoid ? 0 : 1); i++) {
      params.add(Set.of(new SolvedMemberType(paramList.get(i))));
    }

    String paramListAsString = String.join(", ", paramList);
    if (!isVoid) {
      int lastIndexOfComma = paramListAsString.lastIndexOf(',');
      if (lastIndexOfComma != -1) {
        paramListAsString = paramListAsString.substring(0, lastIndexOfComma);
      } else {
        paramListAsString = "";
      }
    }

    Set<String> potentialMethodFQNs = new LinkedHashSet<>();

    for (String fqn : fqns) {
      potentialMethodFQNs.add(fqn + "#apply(" + paramListAsString + ")");
    }

    if (findExistingAndUpdateFQNs(potentialMethodFQNs) != null) {
      // If the method already exists, no need to create a new one
      return List.of(functionalInterface);
    }

    String returnType = isVoid ? "void" : "T" + arity;
    UnsolvedMethodAlternates apply =
        UnsolvedMethodAlternates.create(
            "apply",
            Set.of(new SolvedMemberType(returnType)),
            List.of(functionalInterface),
            params);

    addNewSymbolToGeneratedSymbolsMap(apply);

    return List.of(functionalInterface, apply);
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
   * Given a potential argument expression, this method returns a map of MemberType to
   * CallableDeclaration. For example, if the argument is a method call expression, foo(), as an
   * argument of another method call, bar(foo()), this method will return a map of potential return
   * types of foo() (based on the definitions of bar with an arity of 1) to the CallableDeclaration
   * of bar. This method also works if {@code argument} is a field expression, or if the parent node
   * is a constructor/explicit constructor invocation.
   *
   * @param argument The argument expression to analyze
   * @return A map of potential return types to their corresponding CallableDeclaration. Returns an
   *     empty map if no potential return types are found, or if the argument is not part of a
   *     solvable method/constructor call.
   */
  private Map<MemberType, CallableDeclaration<?>> getTypeToCallableDeclarationFromArgument(
      Expression argument) {
    // If this expression is an argument of a solvable method call, we have multiple potential field
    // types to choose from, based on each definition
    Node parent = argument.getParentNode().get();
    int paramNum = -1;
    Map<MemberType, CallableDeclaration<?>> returnTypeToMustPreserveNode = new LinkedHashMap<>();

    if (!(parent instanceof NodeWithArguments<?> withArgs)) {
      return returnTypeToMustPreserveNode;
    }

    for (int i = 0; i < withArgs.getArguments().size(); i++) {
      if (withArgs.getArgument(i).equals(argument)) {
        paramNum = i;
        break;
      }
    }

    // paramNum could still be -1 if methodCall is the scope of another method call,
    // not an argument
    if (paramNum == -1) {
      return returnTypeToMustPreserveNode;
    }

    List<? extends CallableDeclaration<?>> parentCallableDeclarations =
        JavaParserUtil.tryResolveNodeWithUnresolvableArguments(withArgs, fqnsToCompilationUnits);

    for (CallableDeclaration<?> callable : parentCallableDeclarations) {
      Parameter param = callable.getParameter(paramNum);

      MemberType memberType =
          getOrCreateMemberTypeFromFQNs(
              fullyQualifiedNameGenerator.getFQNsFromType(param.getType()));

      returnTypeToMustPreserveNode.put(memberType, callable);
    }

    return returnTypeToMustPreserveNode;
  }

  /**
   * Replaces all methods with null in their signature to use java.lang.Object instead, and returns
   * the updated methods.
   *
   * @return The updated methods.
   */
  public Set<UnsolvedMethodAlternates> clearMethodsWithNull() {
    for (UnsolvedMethodAlternates unsolvedMethodAlternates : methodsWithNullInSignature) {
      for (UnsolvedMethod alternate : unsolvedMethodAlternates.getAlternates()) {
        alternate.replaceParameterType(
            new SolvedMemberType("null"), SolvedMemberType.JAVA_LANG_OBJECT);
      }

      removeSymbolFromGeneratedSymbolsMap(unsolvedMethodAlternates);
      addNewSymbolToGeneratedSymbolsMap(unsolvedMethodAlternates);
    }

    Set<UnsolvedMethodAlternates> result = Set.copyOf(methodsWithNullInSignature);
    methodsWithNullInSignature.clear();
    return result;
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
                findExistingAndUpdateFQNs(fullyQualifiedNameGenerator.getFQNsFromType(implemented));

        if (syntheticType != null) {
          syntheticType.setType(UnsolvedClassOrInterfaceType.INTERFACE);
        }
      }
      for (ClassOrInterfaceType extended : decl.getExtendedTypes()) {
        UnsolvedClassOrInterfaceAlternates syntheticType =
            (UnsolvedClassOrInterfaceAlternates)
                findExistingAndUpdateFQNs(fullyQualifiedNameGenerator.getFQNsFromType(extended));

        if (syntheticType != null) {
          syntheticType.setType(
              decl.isInterface()
                  ? UnsolvedClassOrInterfaceType.INTERFACE
                  : UnsolvedClassOrInterfaceType.CLASS);
        }
      }
    } else if (node instanceof EnumDeclaration decl) {
      for (ClassOrInterfaceType implemented : decl.getImplementedTypes()) {
        UnsolvedClassOrInterfaceAlternates syntheticType =
            (UnsolvedClassOrInterfaceAlternates)
                findExistingAndUpdateFQNs(fullyQualifiedNameGenerator.getFQNsFromType(implemented));

        if (syntheticType != null) {
          syntheticType.setType(UnsolvedClassOrInterfaceType.INTERFACE);
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
                    fullyQualifiedNameGenerator.getFQNsFromType(
                        thrownException.asClassOrInterfaceType()));

        // Method declaration throws clauses could be either checked or unchecked, but are typically
        // checked exceptions. We'll force checked exceptions (java.lang.Exception) to be first so
        // best-effort generates this as the alternate.
        if (syntheticType != null
            && !syntheticType.doesExtend(SolvedMemberType.JAVA_LANG_EXCEPTION)) {
          // Remove java.lang.Error in case it was added as part of a throw statement (we want
          // the alternate with Exception to generate first)
          syntheticType.removeSuperClass(SolvedMemberType.JAVA_LANG_ERROR);
          syntheticType.forceSuperClass(SolvedMemberType.JAVA_LANG_EXCEPTION);
          syntheticType.forceSuperClass(SolvedMemberType.JAVA_LANG_ERROR);
          toRemove.addAll(handleExtendThrowable(syntheticType));
        }
      }
    } else if (node instanceof ThrowStmt throwStmt) {
      for (FullyQualifiedNameSet fqnSet :
          fullyQualifiedNameGenerator.getFQNsForExpressionType(throwStmt.getExpression())) {
        UnsolvedClassOrInterfaceAlternates syntheticType =
            (UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(fqnSet);

        // If we only see a throw statement, assume it's an unchecked exception until we encounter
        // evidence otherwise (catch, throws clauses)
        if (syntheticType != null && !syntheticType.hasExtends()) {
          syntheticType.forceSuperClass(SolvedMemberType.JAVA_LANG_ERROR);
          toRemove.addAll(handleExtendThrowable(syntheticType));
        }
      }
    } else if (node instanceof TryStmt tryStmt) {
      // Could be null if it is a solved type
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
                      fullyQualifiedNameGenerator.getFQNsFromType(varDeclExpr.getElementType()));
          types.add(lhs);

          for (FullyQualifiedNameSet type :
              fullyQualifiedNameGenerator.getFQNsForExpressionType(
                  varDeclExpr.getVariables().get(0).getInitializer().get())) {
            UnsolvedClassOrInterfaceAlternates rhs =
                (UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(type);

            if (rhs == null) {
              throw new RuntimeException("Unresolved type for resource initializer: " + type);
            }

            types.add(rhs);
          }

        } else if (resource instanceof NameExpr || resource instanceof FieldAccessExpr) {
          for (FullyQualifiedNameSet fqnSet :
              fullyQualifiedNameGenerator.getFQNsForExpressionType((Expression) resource)) {
            UnsolvedClassOrInterfaceAlternates type =
                (UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(fqnSet);

            if (type == null) {
              throw new RuntimeException("Unresolved type for resource initializer: " + fqnSet);
            }

            types.add(type);
          }
        }
      }
      List<@Nullable UnsolvedClassOrInterfaceAlternates> exceptions = new ArrayList<>();
      for (CatchClause clause : tryStmt.getCatchClauses()) {
        Parameter exception = clause.getParameter();

        if (exception.getType().isClassOrInterfaceType()) {
          UnsolvedClassOrInterfaceAlternates type =
              (UnsolvedClassOrInterfaceAlternates)
                  findExistingAndUpdateFQNs(
                      fullyQualifiedNameGenerator.getFQNsFromType(exception.getType()));
          exceptions.add(type);
        } else if (exception.getType().isUnionType()) {
          for (ReferenceType refType : exception.getType().asUnionType().getElements()) {
            UnsolvedClassOrInterfaceAlternates type =
                (UnsolvedClassOrInterfaceAlternates)
                    findExistingAndUpdateFQNs(
                        fullyQualifiedNameGenerator.getFQNsFromType(
                            (ClassOrInterfaceType) refType));
            exceptions.add(type);
          }
        }
      }

      for (UnsolvedClassOrInterfaceAlternates exception : exceptions) {
        if (exception == null || exception.doesExtend(SolvedMemberType.JAVA_LANG_EXCEPTION)) {
          continue;
        }
        // Remove java.lang.Error in case it was added as part of a throw statement (we want
        // the alternate with Exception to generate first)
        exception.removeSuperClass(SolvedMemberType.JAVA_LANG_ERROR);
        exception.forceSuperClass(SolvedMemberType.JAVA_LANG_EXCEPTION);
        exception.forceSuperClass(SolvedMemberType.JAVA_LANG_ERROR);
        toRemove.addAll(handleExtendThrowable(exception));
      }

      for (UnsolvedClassOrInterfaceAlternates type : types) {
        MemberType autoCloseable = new SolvedMemberType("java.lang.AutoCloseable");
        if (type == null || type.doesImplement(autoCloseable)) {
          continue;
        }

        type.forceSuperInterface(autoCloseable);

        UnsolvedMethodAlternates unsolvedMethodAlternates =
            UnsolvedMethodAlternates.create(
                "close",
                Set.of(new SolvedMemberType("void")),
                List.of(type),
                List.of(),
                List.of(SolvedMemberType.JAVA_LANG_EXCEPTION));

        addNewSymbolToGeneratedSymbolsMap(unsolvedMethodAlternates);
        toAdd.add(unsolvedMethodAlternates);
      }
    } else if (node instanceof InstanceOfExpr instanceOf) {
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

      try {
        type.resolve();
        return UnsolvedGenerationResult.EMPTY;
      } catch (UnsolvedSymbolException e) {
        // continue
      }

      Expression relationalExpr = instanceOf.getExpression();

      Set<MemberType> relational =
          getMemberTypesAndExpectNonNullFromFQNSets(
              fullyQualifiedNameGenerator.getFQNsForExpressionType(relationalExpr));

      if (relational.isEmpty()) {
        throw new RuntimeException(
            "Unsolved relational expression when all unsolved symbols should be generated.");
      }

      UnsolvedClassOrInterfaceAlternates referenceType =
          (UnsolvedClassOrInterfaceAlternates)
              findExistingAndUpdateFQNs(fullyQualifiedNameGenerator.getFQNsFromType(type));

      if (referenceType == null) {
        throw new RuntimeException(
            "Unsolved instanceof type when all unsolved symbols should be generated: " + type);
      }

      referenceType.addSuperType(relational);
    }

    // This condition checks to see if the return type of a synthetic method definition
    // can be updated by potential child classes.
    // See VoidReturnDoubleTest for an example of why this is necessary
    else if (node instanceof MethodCallExpr methodCall) {
      matchMethodReturnTypesToKnownChildClasses(methodCall);
    } else if (node instanceof TypeParameter typeParam) {
      // All bounds after the first in a type parameter must be interfaces
      // https://docs.oracle.com/javase/tutorial/java/generics/bounded.html
      List<ClassOrInterfaceType> elements = typeParam.getTypeBound();
      for (int i = 1; i < elements.size(); i++) {
        UnsolvedClassOrInterfaceAlternates syntheticType =
            (UnsolvedClassOrInterfaceAlternates)
                findExistingAndUpdateFQNs(
                    fullyQualifiedNameGenerator.getFQNsFromType(elements.get(i)));

        if (syntheticType != null) {
          syntheticType.setType(UnsolvedClassOrInterfaceType.INTERFACE);
        }
      }
    }

    // Get super classes: type of LHS is a super type of the type of the RHS
    if (node instanceof AssignExpr
        || (node instanceof VariableDeclarator varDecl && varDecl.getInitializer().isPresent())
        || (node instanceof ReturnStmt returnStmt && returnStmt.getExpression().isPresent())
        || node instanceof LambdaExpr) {
      Set<MemberType> lhsType;
      Set<MemberType> rhsType;

      Supplier<ResolvedType> getResolvedTypeOfLHS;

      if (node instanceof AssignExpr assignExpr) {
        Expression lhs = assignExpr.getTarget();
        Expression rhs = assignExpr.getValue();
        lhsType =
            getMemberTypesAndExpectNonNullFromFQNSets(
                fullyQualifiedNameGenerator.getFQNsForExpressionType(lhs));
        rhsType =
            getMemberTypesAndExpectNonNullFromFQNSets(
                fullyQualifiedNameGenerator.getFQNsForExpressionType(rhs));

        getResolvedTypeOfLHS = () -> lhs.calculateResolvedType();
      } else if (node instanceof VariableDeclarator varDecl) {
        Type lhs = varDecl.getType();

        if (lhs.isVarType()) {
          return UnsolvedGenerationResult.EMPTY;
        }

        Expression rhs = varDecl.getInitializer().get();
        MemberType lhsMemberType =
            getMemberTypeFromFQNs(fullyQualifiedNameGenerator.getFQNsFromType(lhs), false);
        lhsType = lhsMemberType == null ? Set.of() : Set.of(lhsMemberType);
        rhsType =
            getMemberTypesAndExpectNonNullFromFQNSets(
                fullyQualifiedNameGenerator.getFQNsForExpressionType(rhs));

        getResolvedTypeOfLHS = () -> lhs.resolve();
      } else if (node instanceof ReturnStmt returnStmt) {
        Node methodOrLambda = JavaParserUtil.findClosestMethodOrLambdaAncestor(returnStmt);

        if (methodOrLambda instanceof MethodDeclaration methodDecl) {
          Type lhs = methodDecl.getType();
          Expression rhs = returnStmt.getExpression().get();
          MemberType lhsMemberType =
              getMemberTypeFromFQNs(fullyQualifiedNameGenerator.getFQNsFromType(lhs), false);
          lhsType = lhsMemberType == null ? Set.of() : Set.of(lhsMemberType);
          rhsType =
              getMemberTypesAndExpectNonNullFromFQNSets(
                  fullyQualifiedNameGenerator.getFQNsForExpressionType(rhs));
          getResolvedTypeOfLHS = () -> lhs.resolve();
        } else {
          // Do not handle here: handle when we encounter the ancestor LambdaExpr node
          return UnsolvedGenerationResult.EMPTY;
        }
      } else if (node instanceof LambdaExpr lambdaExpr) {
        // See if the lambda expression type is available. If not, we can't get a relationship

        ResolvedType solvableTypeFromLambda;
        try {
          ResolvedType functionalInterface = lambdaExpr.calculateResolvedType();

          if (!functionalInterface.isReferenceType()
              || !functionalInterface.asReferenceType().getTypeDeclaration().isPresent()) {
            return UnsolvedGenerationResult.EMPTY;
          }

          ResolvedReferenceTypeDeclaration functionalInterfaceDecl =
              functionalInterface.asReferenceType().getTypeDeclaration().get();

          if (!functionalInterfaceDecl.isFunctionalInterface()) {
            return UnsolvedGenerationResult.EMPTY;
          }

          solvableTypeFromLambda =
              functionalInterfaceDecl.getAllMethods().iterator().next().returnType();
        } catch (UnsolvedSymbolException ex) {
          return UnsolvedGenerationResult.EMPTY;
        }

        if (lambdaExpr.getExpressionBody().isPresent()) {
          MemberType lhsMemberType =
              getMemberTypeFromFQNs(
                  fullyQualifiedNameGenerator.getFQNsForResolvedType(solvableTypeFromLambda),
                  false);
          lhsType = lhsMemberType == null ? Set.of() : Set.of(lhsMemberType);
          rhsType =
              getMemberTypesAndExpectNonNullFromFQNSets(
                  fullyQualifiedNameGenerator.getFQNsForExpressionType(
                      lambdaExpr.getExpressionBody().get()));
          getResolvedTypeOfLHS = () -> solvableTypeFromLambda;
        } else {
          ReturnStmt returnStmt =
              (ReturnStmt)
                  lambdaExpr.getBody().asBlockStmt().stream()
                      .filter(n -> n instanceof ReturnStmt)
                      .findFirst()
                      .orElse(null);

          if (returnStmt == null || !returnStmt.getExpression().isPresent()) {
            return UnsolvedGenerationResult.EMPTY;
          }

          MemberType lhsMemberType =
              getMemberTypeFromFQNs(
                  fullyQualifiedNameGenerator.getFQNsForResolvedType(solvableTypeFromLambda),
                  false);
          lhsType = lhsMemberType == null ? Set.of() : Set.of(lhsMemberType);
          rhsType =
              getMemberTypesAndExpectNonNullFromFQNSets(
                  fullyQualifiedNameGenerator.getFQNsForExpressionType(
                      returnStmt.getExpression().get()));
          getResolvedTypeOfLHS = () -> solvableTypeFromLambda;
        }
      } else {
        throw new RuntimeException("Impossible error");
      }

      if (rhsType.isEmpty()) {
        throw new RuntimeException("Type has not been generated for the RHS of " + node);
      }

      if (lhsType.isEmpty()) {
        throw new RuntimeException("Type has not been generated for the LHS of " + node);
      }

      handleLHSAndRHSRelationship(lhsType, rhsType, getResolvedTypeOfLHS);
    } else if (node instanceof MethodCallExpr
        || node instanceof ObjectCreationExpr
        || node instanceof ExplicitConstructorInvocationStmt
        || (node instanceof EnumConstantDeclaration enumConstantDeclaration
            && enumConstantDeclaration.getArguments().isNonEmpty())) {
      NodeWithArguments<?> nodeWithArgs = (NodeWithArguments<?>) node;

      ResolvedMethodLikeDeclaration resolved;
      try {
        if (!(node instanceof EnumConstantDeclaration)) {
          resolved = (ResolvedMethodLikeDeclaration) ((Resolvable<?>) nodeWithArgs).resolve();

          if (resolved == null) {
            throw new RuntimeException(
                "Resolved declaration is null when it shouldn't be: " + node);
          }
        } else {
          resolved = null;
        }
      } catch (UnsolvedSymbolException ex) {
        resolved = null;
      } catch (UnsupportedOperationException ex) {
        if (node instanceof MethodCallExpr methodCall) {
          Object decl =
              JavaParserUtil.tryFindCorrespondingDeclarationForConstraintQualifiedExpression(
                  methodCall);

          if (decl instanceof ResolvedMethodDeclaration methodDecl) {
            resolved = methodDecl;
          } else {
            throw ex;
          }
        } else {
          throw ex;
        }
      }

      if (resolved != null) {
        CallableDeclaration<?> asAst =
            (CallableDeclaration<?>)
                JavaParserUtil.tryFindAttachedNode(resolved, fqnsToCompilationUnits);

        for (int i = 0; i < nodeWithArgs.getArguments().size(); i++) {
          MemberType lhsType;
          Set<MemberType> rhsType =
              getMemberTypesAndExpectNonNullFromFQNSets(
                  fullyQualifiedNameGenerator.getFQNsForExpressionType(
                      nodeWithArgs.getArgument(i)));

          Supplier<ResolvedType> getResolvedTypeOfLHS;

          try {
            ResolvedParameterDeclaration param;

            if (i >= resolved.getNumberOfParams()) {
              // Varargs; get last param
              param = resolved.getLastParam();
            } else {
              param = resolved.getParam(i);
            }

            lhsType =
                getMemberTypeFromFQNs(
                    fullyQualifiedNameGenerator.getFQNsForResolvedType(param.getType()), false);
            getResolvedTypeOfLHS = () -> param.getType();
          } catch (UnsolvedSymbolException ex) {
            if (asAst == null) {
              // asAst cannot be null here: if the parameter type is unresolvable, then it must be
              // in the project because JDK parameters will always be resolvable
              throw new RuntimeException("asAst cannot be null");
            }

            Type type = asAst.getParameter(i).getType();

            lhsType =
                getMemberTypeFromFQNs(fullyQualifiedNameGenerator.getFQNsFromType(type), false);
            getResolvedTypeOfLHS = () -> type.resolve();
          }

          if (rhsType.isEmpty()) {
            throw new RuntimeException(
                "Type has not been generated for " + nodeWithArgs.getArgument(i));
          }

          if (lhsType == null) {
            throw new RuntimeException(
                "Type has not been generated for the LHS of parameter " + i + " of " + node);
          }

          handleLHSAndRHSRelationship(Set.of(lhsType), rhsType, getResolvedTypeOfLHS);
        }
      } else {
        List<? extends CallableDeclaration<?>> withUnresolvableArgs =
            JavaParserUtil.tryResolveNodeWithUnresolvableArguments(
                nodeWithArgs, fqnsToCompilationUnits);

        if (withUnresolvableArgs.isEmpty()) {
          UnsolvedMethodAlternates genMethod = null;
          if (node instanceof MethodCallExpr methodCall) {
            Collection<Set<String>> methodScope =
                fullyQualifiedNameGenerator.getFQNsForExpressionLocation(methodCall);

            // Could be empty if the method is called on a NameExpr with a union type,
            // but the method is located in a known class.
            if (methodScope.isEmpty()) {
              return UnsolvedGenerationResult.EMPTY;
            }

            for (Set<String> set : methodScope) {
              if (doesOverlapWithKnownType(set)) {
                return UnsolvedGenerationResult.EMPTY;
              }
            }

            Set<String> methodFqns =
                fullyQualifiedNameGenerator.generateMethodFQNsWithSideEffect(
                    methodCall, methodScope, null, false);
            genMethod = (UnsolvedMethodAlternates) findExistingAndUpdateFQNs(methodFqns);

            // If there is a null, and the Object version is not findable, then another call to the
            // same method exists, and we'll get the signature from there instead
            if (genMethod == null
                && (isMethodABuiltInThrowableMethod(methodScope, methodFqns)
                    || methodCall.getArguments().stream()
                        .anyMatch(Expression::isNullLiteralExpr))) {
              return UnsolvedGenerationResult.EMPTY;
            }
          } else {
            genMethod =
                (UnsolvedMethodAlternates)
                    findExistingAndUpdateFQNs(getFQNsForUnsolvableConstructor(node));
          }

          if (genMethod == null) {
            throw new RuntimeException("Method alternates for " + node + " could not be found");
          }

          for (int i = 0; i < nodeWithArgs.getArguments().size(); i++) {
            final int iCopy = i;
            Set<MemberType> lhsType =
                genMethod.getAlternates().stream()
                    .map(alt -> alt.getParameterList().get(iCopy))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<MemberType> rhsType =
                getMemberTypesAndExpectNonNullFromFQNSets(
                    fullyQualifiedNameGenerator.getFQNsForExpressionType(
                        nodeWithArgs.getArgument(i)));

            // If the method is a synthetic definition, there is no resolved type of the LHS
            Supplier<ResolvedType> getResolvedTypeOfLHS =
                () -> {
                  throw new UnsolvedSymbolException("");
                };

            if (rhsType.isEmpty()) {
              throw new RuntimeException(
                  "Type has not been generated for " + nodeWithArgs.getArgument(i));
            }

            if (lhsType.isEmpty()) {
              throw new RuntimeException(
                  "Type has not been generated for the LHS of parameter " + i + " of " + node);
            }

            handleLHSAndRHSRelationship(lhsType, rhsType, getResolvedTypeOfLHS);
          }
        } else {
          for (int i = 0; i < nodeWithArgs.getArguments().size(); i++) {
            final int iCopy = i;
            Set<MemberType> lhsType =
                withUnresolvableArgs.stream()
                    .map(
                        alt -> {
                          MemberType paramType =
                              getMemberTypeFromFQNs(
                                  fullyQualifiedNameGenerator.getFQNsFromType(
                                      alt.getParameter(iCopy).getType()),
                                  false);

                          if (paramType == null) {
                            throw new RuntimeException(
                                "Parameter type could not be resolved for "
                                    + alt.getParameter(iCopy));
                          }

                          return paramType;
                        })
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Set<MemberType> rhsType =
                getMemberTypesAndExpectNonNullFromFQNSets(
                    fullyQualifiedNameGenerator.getFQNsForExpressionType(
                        nodeWithArgs.getArgument(i)));

            // Unless there is only one LHS possibility, we cannot resolve the type
            Supplier<ResolvedType> getResolvedTypeOfLHS;

            if (withUnresolvableArgs.size() == 1) {
              getResolvedTypeOfLHS =
                  () -> withUnresolvableArgs.get(0).getParameter(iCopy).getType().resolve();
            } else {
              getResolvedTypeOfLHS =
                  () -> {
                    throw new UnsolvedSymbolException("");
                  };
            }

            if (rhsType.isEmpty()) {
              throw new RuntimeException(
                  "Type has not been generated for " + nodeWithArgs.getArgument(i));
            }

            if (lhsType.isEmpty()) {
              throw new RuntimeException(
                  "Type has not been generated for the LHS of parameter " + i + " of " + node);
            }

            handleLHSAndRHSRelationship(lhsType, rhsType, getResolvedTypeOfLHS);
          }
        }
      }
    }

    return new UnsolvedGenerationResult(toAdd, toRemove);
  }

  /**
   * Given a method call expression, try to match its return types to known child classes if this
   * method declaration behind the call matches all of these requirements:
   *
   * <ul>
   *   <li>The method has an unsolved super method declaration with a generated synthetic definition
   *   <li>There are known child classes of the unsolved declaring type of this method
   * </ul>
   *
   * If all these requirements are matched, then we update the return type of the method declaration
   * based on the known child class method override return type. We also remove all instances of the
   * synthetic return type. If multiple child classes are found, then we will find the least upper
   * bound of all these return types.
   *
   * <p>If any of these requirements are not matched, then we return early and nothing gets changed.
   *
   * @param methodCall The method call expression to analyze
   */
  private void matchMethodReturnTypesToKnownChildClasses(MethodCallExpr methodCall) {
    Collection<Set<String>> potentialScopeFQNs = null;
    ResolvedMethodDeclaration resolvedMethod = null;
    MethodDeclaration ast = null;
    try {
      resolvedMethod = methodCall.resolve();
    } catch (UnsolvedSymbolException ex) {
      ast =
          (MethodDeclaration)
              JavaParserUtil.tryFindSingleCallableForNodeWithUnresolvableArguments(
                  methodCall, fqnsToCompilationUnits);

      if (ast == null) {
        potentialScopeFQNs = fullyQualifiedNameGenerator.getFQNsForExpressionLocation(methodCall);
      }
    } catch (UnsupportedOperationException ex) {
      resolvedMethod =
          (ResolvedMethodDeclaration)
              JavaParserUtil.tryFindCorrespondingDeclarationForConstraintQualifiedExpression(
                  methodCall);
    }

    if (resolvedMethod != null) {
      // Potential scope is all unsolvable ancestors
      ast =
          (MethodDeclaration)
              JavaParserUtil.tryFindAttachedNode(resolvedMethod, fqnsToCompilationUnits);
      if (ast == null) {
        return;
      }
    }

    if (ast != null) {
      List<ClassOrInterfaceType> unsolvableAncestors =
          JavaParserUtil.getAllUnsolvableAncestors(
              JavaParserUtil.getEnclosingClassLike(ast), fqnsToCompilationUnits);

      if (unsolvableAncestors.isEmpty()) {
        return;
      }

      potentialScopeFQNs = new ArrayList<>();
      for (ClassOrInterfaceType ancestor : unsolvableAncestors) {
        potentialScopeFQNs.add(fullyQualifiedNameGenerator.getFQNsFromType(ancestor).erasedFqns());
      }
    }

    // Could be empty if the method is called on a NameExpr with a union type,
    // but the method is located in a known class.
    if (potentialScopeFQNs == null || potentialScopeFQNs.isEmpty()) {
      return;
    }

    for (Set<String> set : potentialScopeFQNs) {
      if (doesOverlapWithKnownType(set)) {
        return;
      }
    }

    Set<String> potentialFQNs =
        fullyQualifiedNameGenerator.generateMethodFQNsWithSideEffect(
            methodCall, potentialScopeFQNs, null, false);

    UnsolvedMethodAlternates alt =
        (UnsolvedMethodAlternates) findExistingAndUpdateFQNs(potentialFQNs);

    if (alt == null) {
      if (isMethodABuiltInThrowableMethod(potentialScopeFQNs, potentialFQNs)
          || resolvedMethod != null
          || ast != null) {
        return;
      }

      // If there is a null, and the Object version is not findable, then another call to the same
      // method exists, and we'll get the signature from there instead
      if (methodCall.getArguments().stream().anyMatch(Expression::isNullLiteralExpr)) {
        return;
      }
      throw new RuntimeException(
          "Unresolvable method is not generated when all unsolved symbols should be: "
              + potentialFQNs);
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
          return;
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
      methodSignature = methodSignature.substring(potentialFQNs.iterator().next().indexOf('#') + 1);

      List<ResolvedType> resolvedReturnTypes = new ArrayList<>();
      List<UnsolvedMemberType> unsolvedReturnTypes = new ArrayList<>();

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
              try {
                resolvedReturnTypes.add(methodDecl.getReturnType());
              } catch (UnsolvedSymbolException ex) {
                MemberType returnType =
                    getMemberTypeFromFQNs(
                        fullyQualifiedNameGenerator.getFQNsFromType(methodDeclAst.getType()),
                        false);

                if (returnType == null) {
                  throw new RuntimeException(
                      "Unsolved return type when all types should be generated: "
                          + methodDeclAst.getType());
                }
                unsolvedReturnTypes.add((UnsolvedMemberType) returnType);
              }
            }
          }
        }
      }

      // Note that resolvedReturnTypes and solvedReturnTypes do not contain all the possible return
      // types. Typically, it'll only contain one type in total (i.e., the return type of the
      // method) directly corresponding with the current method call. Here, we'll add the inferred
      // return type of the unsolved super type's method, or the previously calculated lub, and
      // recalculate the new lub.
      for (MemberType returnType : alt.getReturnTypes()) {
        if (resolvedReturnTypes.isEmpty() && returnType instanceof UnsolvedMemberType unsolved) {
          if (unsolvedReturnTypes.isEmpty()) {
            // nothing to do
            continue;
          }

          // In this case, set lub to the first encounter
          UnsolvedMemberType lub = unsolvedReturnTypes.get(0);

          for (int i = 1; i < unsolvedReturnTypes.size(); i++) {
            unsolvedReturnTypes.get(i).getUnsolvedType().addSuperType(Set.of(lub));
          }

          unsolved.getUnsolvedType().addSuperType(Set.of(lub));

          alt.replaceReturnType(returnType, lub);
        } else {
          List<SolvedMemberType> solvedReturnTypeAsList =
              returnType instanceof SolvedMemberType solved ? List.of(solved) : List.of();

          if (resolvedReturnTypes.isEmpty()) {
            // The current return type is equal to the least upper bound, so we don't need to do
            // anything
            continue;
          }

          ResolvedReferenceTypeDeclaration lub =
              JavaParserUtil.getLeastUpperBound(resolvedReturnTypes, solvedReturnTypeAsList);

          if (lub == null) {
            boolean found = false;
            // If null, then a type is a primitive/void
            for (ResolvedType type : resolvedReturnTypes) {
              alt.replaceReturnType(returnType, new SolvedMemberType(type.describe()));
              found = true;
              break;
            }

            if (!found) {
              for (SolvedMemberType solved : solvedReturnTypeAsList) {
                String type = solved.getFullyQualifiedNames().iterator().next();
                if (JavaLangUtils.isPrimitive(type) || type.equals("void")) {
                  alt.replaceReturnType(returnType, solved);
                  break;
                }
              }
            }
          } else {
            // Set type parameters to make sure we implement/extend the generic version, not the raw
            // type
            SolvedMemberType asSolvedMemberType =
                new SolvedMemberType(
                    lub.getQualifiedName(),
                    Collections.nCopies(
                        lub.getTypeParameters().size(), WildcardMemberType.UNBOUNDED));

            if (unsolvedReturnTypes.isEmpty()) {
              alt.replaceReturnType(returnType, asSolvedMemberType);
            }
          }
        }
      }
    }
  }

  /**
   * Given the possible declaring type fully qualified names and potential method call FQNs, check
   * to see if this is defined in java.lang.Throwable.
   *
   * @param potentialScopeFQNs The potential declaring type fully qualified names
   * @param potentialFQNs The potential method call fully qualified names
   * @return True if this method call is a java.lang.Throwable method
   */
  private boolean isMethodABuiltInThrowableMethod(
      Collection<Set<String>> potentialScopeFQNs, Set<String> potentialFQNs) {
    for (Set<String> set : potentialScopeFQNs) {
      UnsolvedClassOrInterfaceAlternates generatedType =
          (UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(set);
      if (generatedType != null
          && (generatedType.doesExtend(SolvedMemberType.JAVA_LANG_EXCEPTION)
              || generatedType.doesExtend(SolvedMemberType.JAVA_LANG_ERROR))) {
        if (potentialFQNs.stream()
            .map(fqn -> fqn.substring(fqn.indexOf('#') + 1))
            .anyMatch(fqn -> JavaLangUtils.getJavaLangThrowableMethods().containsKey(fqn))) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Handles the relationship between the LHS and RHS types by making the type of the LHS a
   * supertype of the type of the RHS, if the type of the RHS is unsolved.
   *
   * @param lhsTypes The type(s) of the LHS
   * @param rhsTypes The type(s) of the RHS
   * @param getResolvedTypeOfLHS A supplier for the resolved type of the LHS. Typically a call to
   *     resolve() or calculateResolvedType().
   */
  private void handleLHSAndRHSRelationship(
      Set<MemberType> lhsTypes,
      Set<MemberType> rhsTypes,
      Supplier<ResolvedType> getResolvedTypeOfLHS) {

    @Nullable ResolvedType resolved;
    try {
      resolved = getResolvedTypeOfLHS.get();
    } catch (UnsolvedSymbolException ex) {
      resolved = null;
    }

    // Make sure all erasures of the RHS types are handled with the LHS types
    for (MemberType rhsType : rhsTypes) {
      // If RHS is solvable, do not continue
      if (!(rhsType instanceof UnsolvedMemberType unsolved)) {
        continue;
      }

      if (resolved != null) {
        if (resolved.isReferenceType()
            && resolved.asReferenceType().getTypeDeclaration().isPresent()) {
          ResolvedReferenceTypeDeclaration decl =
              resolved.asReferenceType().getTypeDeclaration().get();

          // If LHS is solvable, there is only one
          if (decl.isClass()) {
            unsolved.getUnsolvedType().forceSuperClass(lhsTypes.iterator().next());
          } else if (decl.isInterface()) {
            unsolved.getUnsolvedType().forceSuperInterface(lhsTypes.iterator().next());
          } else {
            throw new RuntimeException("Invalid LHS type: " + resolved.describe());
          }
        }
      } else {
        unsolved.getUnsolvedType().addSuperType(lhsTypes);
      }
    }

    // Now, make sure the type parameters also have the same relationship, if the left hand side is
    // a bounded wildcard
    if (resolved != null) {
      if (!resolved.isReferenceType()) {
        return;
      }

      List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> typeParameters =
          resolved.asReferenceType().getTypeParametersMap();
      for (int i = 0; i < typeParameters.size(); i++) {
        ResolvedType typeParam = typeParameters.get(i).b;

        if (typeParam.isWildcard() && typeParam.asWildcard().isBounded()) {
          ResolvedType bound = typeParam.asWildcard().getBoundedType();
          boolean isUpperBound = typeParam.asWildcard().isUpperBounded();

          String erased = JavaParserUtil.erase(resolved.describe());

          Set<MemberType> rhsTypeParameters = new LinkedHashSet<>();
          for (MemberType rhsType : rhsTypes) {
            // There are many possibilities for this: for example, if rhsType is a raw type,
            // if rhsType is a non-generic type that extends a generic type, etc.
            if (rhsType.getTypeArguments().size() <= i) {
              continue;
            }

            MemberType typeArg = rhsType.getTypeArguments().get(i);

            if (typeArg instanceof WildcardMemberType rhsWildcard) {
              MemberType memberTypeBound = rhsWildcard.getBound();

              if (memberTypeBound != null) {
                typeArg = memberTypeBound;
              }
            }

            if (!rhsType.getFullyQualifiedNames().stream().anyMatch(erased::contains)) {
              continue;
            }

            rhsTypeParameters.add(typeArg);
          }

          // ? extends with ? extends; there is no ? extends with ? super
          if (isUpperBound) {
            MemberType memberTypeBound = lhsTypes.iterator().next().getTypeArguments().get(i);

            memberTypeBound = ((WildcardMemberType) memberTypeBound).getBound();

            if (memberTypeBound == null) {
              throw new RuntimeException(
                  "Null member type wildcard bound when resolved wildcard bound is not null");
            }

            handleLHSAndRHSRelationship(Set.of(memberTypeBound), rhsTypeParameters, () -> bound);
          } else {
            // If the LHS were unsolved, we would make it extend every single class in the RHS; but
            // since the LHS is solved, we can't do this

            // If an issue arises in the future, we could find the unsolvable super classes of this
            // resolvable type bound and then apply these bounds there
          }
        }
      }

      return;
    }

    for (MemberType lhsType : lhsTypes) {
      for (int i = 0; i < lhsType.getTypeArguments().size(); i++) {
        MemberType typeParam = lhsType.getTypeArguments().get(i);

        if (!(typeParam instanceof WildcardMemberType wildcard)
            || wildcard.equals(WildcardMemberType.UNBOUNDED)) {
          continue;
        }

        MemberType bound = wildcard.getBound();

        if (bound == null) {
          continue;
        }
        boolean isUpperBound = wildcard.isUpperBounded();

        Set<String> erased =
            lhsType.getFullyQualifiedNames().stream()
                .map(JavaParserUtil::erase)
                .collect(Collectors.toSet());

        Set<MemberType> rhsTypeParameters = new LinkedHashSet<>();
        for (MemberType rhsType : rhsTypes) {
          // There are many possibilities for this: for example, if rhsType is a raw type,
          // if rhsType is a non-generic type that extends a generic type, etc.
          if (rhsType.getTypeArguments().size() <= i) {
            continue;
          }

          MemberType typeArg = rhsType.getTypeArguments().get(i);

          if (typeArg instanceof WildcardMemberType rhsWildcard) {
            MemberType memberTypeBound = rhsWildcard.getBound();

            if (memberTypeBound != null) {
              typeArg = memberTypeBound;
            }
          }

          if (!rhsType.getFullyQualifiedNames().stream().anyMatch(erased::contains)) {
            continue;
          }

          rhsTypeParameters.add(typeArg);
        }

        // ? extends with ? extends; there is no ? extends with ? super
        if (isUpperBound) {
          handleLHSAndRHSRelationship(
              Set.of(bound),
              rhsTypeParameters,
              () -> {
                throw new UnsolvedSymbolException("");
              });
        } else {
          handleLHSAndRHSRelationship(
              rhsTypeParameters,
              rhsTypes,
              () -> {
                throw new UnsolvedSymbolException("");
              });
        }
      }
    }
  }

  /**
   * Returns the FQNs for an unsolvable constructor call.
   *
   * @param node The node representing the constructor call; either an ObjectCreationExpr or
   *     ExplicitConstructorInvocationStmt
   * @return A set of FQNs representing the constructor
   */
  private Set<String> getFQNsForUnsolvableConstructor(Node node) {
    UnsolvedClassOrInterfaceAlternates scope;
    String constructorName;
    List<Expression> arguments;

    if (node instanceof ObjectCreationExpr constructor) {
      scope =
          (UnsolvedClassOrInterfaceAlternates)
              findExistingAndUpdateFQNs(
                  fullyQualifiedNameGenerator.getFQNsFromType(constructor.getType()));

      constructorName = constructor.getTypeAsString();
      arguments = constructor.getArguments();
    } else if (node instanceof ExplicitConstructorInvocationStmt constructor) {
      // If it's unresolvable, it's a constructor in the unsolved parent class
      if (!constructor.isThis()) {
        // There can only be one extends in a class
        ClassOrInterfaceType superClass = JavaParserUtil.getSuperClass(node);

        scope =
            (UnsolvedClassOrInterfaceAlternates)
                findExistingAndUpdateFQNs(fullyQualifiedNameGenerator.getFQNsFromType(superClass));

        constructorName = superClass.getNameAsString();
        arguments = constructor.getArguments();
      } else {
        // We should never reach this case unless the user inputted a bad program (i.e.
        // this(...) constructor call when a definition is not there, or super() without a parent
        // class)
        throw new RuntimeException("Unexpected explicit constructor invocation statement call.");
      }
    } else {
      throw new RuntimeException(
          "Parameter node must be an ObjectCreationExpr or an ExplicitConstructorInvocationStmt: "
              + node.getClass());
    }

    if (scope == null) {
      throw new RuntimeException("Scope not created when it should've been");
    }

    constructorName =
        JavaParserUtil.getSimpleNameFromQualifiedName(JavaParserUtil.erase(constructorName));

    List<Set<String>> simpleNames = new ArrayList<>();

    for (Expression argument : arguments) {
      Set<String> simpleNamesForArgument = new LinkedHashSet<>();
      for (FullyQualifiedNameSet fqns :
          fullyQualifiedNameGenerator.getFQNsForExpressionType(argument)) {
        String first = fqns.erasedFqns().iterator().next();
        simpleNamesForArgument.add(JavaParserUtil.getSimpleNameFromQualifiedName(first));
      }
      simpleNames.add(simpleNamesForArgument);
    }

    Set<String> potentialFQNs = new LinkedHashSet<>();

    for (List<String> simpleNameList : JavaParserUtil.generateAllCombinations(simpleNames)) {
      for (String potentialScopeFQN : scope.getFullyQualifiedNames()) {
        potentialFQNs.add(
            potentialScopeFQN
                + "#"
                + constructorName
                + "("
                + String.join(", ", simpleNameList)
                + ")");
      }
    }

    return potentialFQNs;
  }

  /**
   * Once {@link #addInformation(Node)} is done, call this method to make sure all generated symbols
   * are consistent with their super type relationships.
   */
  public void generateAllAlternatesBasedOnSuperTypeRelationships() {
    // This method is called after all unsolved symbols are generated and all information is added
    // to ensure that all symbols are consistent with their super type relationships.
    for (UnsolvedSymbolAlternates<?> symbol : Set.copyOf(generatedSymbols.values())) {
      if (symbol instanceof UnsolvedClassOrInterfaceAlternates type) {
        type.createAlternatesBasedOnSuperTypeRelationships();
      }
    }
  }

  /**
   * Call this the first time a type is set to extend a Throwable (Exception, Error, itself, etc.).
   * This removes all methods that may have been generated for the type but also exists in the
   * Throwable class. This is an expensive call.
   *
   * @param type The type that extends Throwable
   * @return Symbols that need to be removed
   */
  private List<UnsolvedSymbolAlternates<?>> handleExtendThrowable(
      UnsolvedClassOrInterfaceAlternates type) {
    // Remove all methods that are already defined in Throwable
    // This is because the type is now a Throwable, so it cannot have its own methods
    // that are already defined in Throwable.

    // Method to remove to the proper signature
    Map<UnsolvedMethodAlternates, String> methodsToRemove = new HashMap<>();
    Map<String, String> methods = JavaLangUtils.getJavaLangThrowableMethods();

    for (UnsolvedSymbolAlternates<?> symbol : generatedSymbols.values()) {
      if (symbol instanceof UnsolvedMethodAlternates method) {
        if (method.getAlternateDeclaringTypes().contains(type)) {
          String fqn =
              method.getFullyQualifiedNames().stream()
                  .map(f -> f.substring(f.indexOf('#') + 1))
                  .filter(f -> methods.containsKey(f))
                  .findFirst()
                  .orElse(null);
          if (fqn != null) {
            methodsToRemove.put(method, fqn);
          }
        }
      }
    }

    Map<UnsolvedClassOrInterfaceAlternates, SolvedMemberType> typeCorrect = new HashMap<>();
    for (Entry<UnsolvedMethodAlternates, String> entry : methodsToRemove.entrySet()) {
      UnsolvedMethodAlternates method = entry.getKey();
      String methodSignature = entry.getValue();
      String correctReturnType = methods.get(methodSignature);

      if (correctReturnType == null) {
        throw new RuntimeException("Unknown method signature: " + methodSignature);
      }

      // Remove all instances of the synthetic return type
      for (MemberType returnType : method.getReturnTypes()) {
        if (returnType instanceof UnsolvedMemberType unsolvedReturn) {
          UnsolvedClassOrInterfaceAlternates unsolvedType = unsolvedReturn.getUnsolvedType();
          typeCorrect.put(unsolvedType, new SolvedMemberType(correctReturnType));
        }
      }
    }

    Set<String> keysToRemove = new HashSet<>();
    Set<UnsolvedMethodAlternates> methodsWithChangedSignatures = new HashSet<>();
    for (UnsolvedSymbolAlternates<?> symbol : generatedSymbols.values()) {
      if (symbol instanceof UnsolvedMethodAlternates method) {
        for (MemberType returnType : method.getReturnTypes()) {
          if (returnType instanceof UnsolvedMemberType unsolvedReturn) {
            UnsolvedClassOrInterfaceAlternates unsolvedType = unsolvedReturn.getUnsolvedType();
            SolvedMemberType correct = typeCorrect.get(unsolvedType);
            if (correct != null) {
              method.replaceReturnType(unsolvedReturn, correct);
            }
          }
        }

        Set<String> oldSignatures = method.getFullyQualifiedNames();
        boolean signatureChanged = false;
        for (UnsolvedMethod alternate : method.getAlternates()) {
          for (MemberType paramType : alternate.getParameterList()) {
            if (paramType instanceof UnsolvedMemberType unsolvedParam) {
              UnsolvedClassOrInterfaceAlternates unsolvedType = unsolvedParam.getUnsolvedType();
              SolvedMemberType correct = typeCorrect.get(unsolvedType);
              if (correct != null) {
                alternate.replaceParameterType(unsolvedParam, correct);
                signatureChanged = true;
              }
            }
          }
        }

        if (signatureChanged) {
          keysToRemove.addAll(oldSignatures);
          methodsWithChangedSignatures.add(method);
        }

        method.removeDuplicateAlternates();
      } else if (symbol instanceof UnsolvedFieldAlternates field) {
        for (MemberType fieldType : field.getTypes()) {
          if (fieldType instanceof UnsolvedMemberType unsolvedType) {
            UnsolvedClassOrInterfaceAlternates unsolvedClass = unsolvedType.getUnsolvedType();
            SolvedMemberType correct = typeCorrect.get(unsolvedClass);
            if (correct != null) {
              field.replaceFieldType(unsolvedType, correct);
            }
          }
        }

        field.removeDuplicateAlternates();
      }
    }

    for (String signatureToRemove : keysToRemove) {
      generatedSymbols.remove(signatureToRemove);
    }

    for (UnsolvedMethodAlternates method : methodsWithChangedSignatures) {
      addNewSymbolToGeneratedSymbolsMap(method);
    }

    List<UnsolvedSymbolAlternates<?>> toRemove = new ArrayList<>(methodsToRemove.keySet());
    toRemove.addAll(typeCorrect.keySet());

    for (UnsolvedSymbolAlternates<?> symbol : toRemove) {
      removeSymbolFromGeneratedSymbolsMap(symbol);
    }

    return toRemove;
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
        || node instanceof EnumDeclaration
        || node instanceof MethodDeclaration
        || node instanceof TryStmt
        || node instanceof ThrowStmt
        || node instanceof InstanceOfExpr
        || node instanceof MethodCallExpr
        || node instanceof TypeParameter
        || node instanceof AssignExpr
        || node instanceof ReturnStmt
        || node instanceof VariableDeclarator
        || node instanceof LambdaExpr
        || node instanceof ObjectCreationExpr
        || node instanceof ExplicitConstructorInvocationStmt
        || node instanceof EnumConstantDeclaration;
  }

  /**
   * Converts a set of FullyQualifiedNameSet to a set of MemberType. Throws if any
   * FullyQualifiedNameSet doesn't correspond with a generated MemberType.
   *
   * @param fqnSets The set of FullyQualifiedNameSet to convert.
   * @return A set of MemberType corresponding to the input FQNSets.
   */
  private Set<MemberType> getMemberTypesAndExpectNonNullFromFQNSets(
      Set<FullyQualifiedNameSet> fqnSets) {
    Set<MemberType> memberTypes = new LinkedHashSet<>();

    for (FullyQualifiedNameSet fqnSet : fqnSets) {
      MemberType genType = getMemberTypeFromFQNs(fqnSet, false);

      if (genType == null) {
        throw new RuntimeException("Unresolved type when we expect a generated type: " + fqnSet);
      }

      memberTypes.add(genType);
    }

    return memberTypes;
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
   * Short-hand call for {@link #findExistingAndUpdateFQNs(FullyQualifiedNameSet)} that takes a
   * {@link FullyQualifiedNameSet} as input.
   *
   * @param potentialFQNs The set of potential FQNs
   * @return The existing symbol, or null if one does not exist yet.
   * @see #findExistingAndUpdateFQNs(Set)
   */
  private @Nullable UnsolvedSymbolAlternates<?> findExistingAndUpdateFQNs(
      FullyQualifiedNameSet potentialFQNs) {
    return findExistingAndUpdateFQNs(potentialFQNs.erasedFqns());
  }

  /**
   * Finds the existing unsolved symbol based on a set of potential FQNs. If none is found, this
   * method returns null. The generatedSymbols map is also modified if the intersection of
   * potentialFQNs and the existing set results in a smaller set of potential FQNs.
   *
   * @param potentialFQNs The set of potential fully-qualified names (type arguments erased) in the
   *     current context.
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

        Set<String> typeFQNs = potentialFQNs;

        if (!(alreadyGenerated instanceof UnsolvedClassOrInterfaceAlternates)) {
          typeFQNs =
              potentialFQNs.stream()
                  .map(fqn -> fqn.substring(0, fqn.indexOf('#')))
                  .collect(Collectors.toSet());
        }

        // TODO before you push this commit: for methods, only return alreadyGenerated if
        // the parameter types match too. If the input is a subset of all the fqns, then
        // this is likely another method that we have to generate (this can happen when
        // there are ambiguous method references passed in as an argument)

        type.updateFullyQualifiedNames(typeFQNs);

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
  private MemberType getOrCreateMemberTypeFromFQNs(FullyQualifiedNameSet fqns) {
    MemberType memberType = getMemberTypeFromFQNs(fqns, true);

    if (memberType == null) {
      throw new RuntimeException("This error is impossible.");
    }

    return memberType;
  }

  /**
   * Returns true if any fqn in the set represents a type included in the input or in the JDK.
   *
   * @param fqns The set of fully-qualified names to check
   * @return True if the set overlaps with known types, false otherwise
   */
  private boolean doesOverlapWithKnownType(Set<String> fqns) {
    for (String fqn : fqns) {
      if (fqnsToCompilationUnits.containsKey(fqn)
          || JavaLangUtils.inJdkPackage(JavaParserUtil.removeArrayBrackets(fqn))
          || JavaLangUtils.isJavaLangOrPrimitiveName(
              JavaParserUtil.getSimpleNameFromQualifiedName(
                  JavaParserUtil.removeArrayBrackets(fqn)))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gets the {@code MemberType} from a set of FQNs. If one of the FQNs represents a primitive or
   * built-in java class, then it returns that type. If not, then this method will find an existing
   * generated type (or create it, depending on {@code createNew}), and return it. If there are type
   * arguments, please fully qualify them before passing into this method.
   *
   * @param fqns The set of fully-qualified names
   * @return The member type
   */
  private @Nullable MemberType getMemberTypeFromFQNs(
      FullyQualifiedNameSet fqns, boolean createNew) {
    String wildcard = fqns.wildcard();
    if (wildcard != null) {
      if (wildcard.equals(FullyQualifiedNameSet.UNBOUNDED_WILDCARD.wildcard())) {
        return WildcardMemberType.UNBOUNDED;
      }

      if (wildcard.equals("? extends")) {
        return new WildcardMemberType(
            getMemberTypeFromFQNs(
                new FullyQualifiedNameSet(fqns.erasedFqns(), fqns.typeArguments()), createNew),
            true);
      } else if (wildcard.equals("? super")) {
        return new WildcardMemberType(
            getMemberTypeFromFQNs(
                new FullyQualifiedNameSet(fqns.erasedFqns(), fqns.typeArguments()), createNew),
            false);
      }

      throw new RuntimeException("Unexpected wildcard: " + wildcard);
    }

    List<MemberType> typeArguments = new ArrayList<>();

    for (FullyQualifiedNameSet typeArg : fqns.typeArguments()) {
      MemberType memberType = getMemberTypeFromFQNs(typeArg, createNew);

      if (memberType == null) {
        throw new RuntimeException("Type arguments must be generated.");
      }

      typeArguments.add(memberType);
    }

    for (String fqn : fqns.erasedFqns()) {
      if (fqnsToCompilationUnits.containsKey(JavaParserUtil.removeArrayBrackets(fqn))) {
        return new SolvedMemberType(fqn, typeArguments);
      }

      MemberType type = getMemberTypeIfPrimitiveOrJavaLang(fqn, typeArguments);

      if (type != null) {
        return type;
      }
    }

    // If a set has one element with no dots, it's likely a type variable
    if (fqns.erasedFqns().size() == 1 && !fqns.erasedFqns().iterator().next().contains(".")) {
      return new SolvedMemberType(fqns.erasedFqns().iterator().next());
    }

    UnsolvedClassOrInterfaceAlternates unsolved;

    Set<String> fqnsWithoutArray = new LinkedHashSet<>();

    for (String fqn : fqns.erasedFqns()) {
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
          unsolved,
          JavaParserUtil.countNumberOfArrayBrackets(fqns.erasedFqns().iterator().next()),
          typeArguments);
    }
  }

  /**
   * If {@code name} (either a simple name or fully qualified) is primitive, java.lang, or in
   * another java package, then return the MemberType holding it. Else, return null.
   *
   * @param name The name of the type, either simple or fully qualified.
   * @param typeArguments The type arguments of the type, if any.
   */
  private @Nullable MemberType getMemberTypeIfPrimitiveOrJavaLang(
      String name, List<MemberType> typeArguments) {
    if (JavaLangUtils.inJdkPackage(JavaParserUtil.removeArrayBrackets(name))
        || JavaLangUtils.isJavaLangOrPrimitiveName(
            JavaParserUtil.getSimpleNameFromQualifiedName(JavaParserUtil.removeArrayBrackets(name)))
        || name.equals("void")) {
      return new SolvedMemberType(name, typeArguments);
    }
    return null;
  }
}
