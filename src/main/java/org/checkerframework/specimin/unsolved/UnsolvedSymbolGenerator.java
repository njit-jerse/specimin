package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.AccessSpecifier;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
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
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.CastExpr;
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
import com.github.javaparser.ast.nodeTypes.NodeWithParameters;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
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
import com.github.javaparser.resolution.declarations.ResolvedAnnotationDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.specimin.JavaLangUtils;
import org.checkerframework.specimin.JavaParserUtil;
import org.checkerframework.specimin.Resolver;

/**
 * Generates unsolved symbols. This class ensures that only one of each type is created; i.e., the
 * same FQNs will point to the same instance. More symbols are tracked here than returned into the
 * final slice; this is to ensure classes used by some alternates are only outputted when those
 * alternates are selected.
 */
public class UnsolvedSymbolGenerator {
  /**
   * The type given to a synthetic field that is used as an annotation argument but whose declaring
   * type is not an enum. JLS 9.7.1 requires such an argument to be a constant expression, so the
   * field's type must be primitive or String; {@code int} is as good a choice as any.
   */
  private static final SolvedMemberType ANNOTATION_CONSTANT_TYPE = new SolvedMemberType("int");

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

  // This warning is triggered on the lambda, but the lambda is always called after the constructor
  // finishes, so it's safe
  @SuppressWarnings("nullness:method.invocation")
  public UnsolvedSymbolGenerator(Map<String, CompilationUnit> fqnsToCompilationUnits) {
    this.fqnsToCompilationUnits = fqnsToCompilationUnits;

    fullyQualifiedNameGenerator =
        new FullyQualifiedNameGenerator(
            fqnsToCompilationUnits, generatedSymbols, (fqns) -> getMemberTypeFromFQNs(fqns, false));
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
      // A method reference scope that names a variable is a TypeExpr too, but its name is the
      // variable's, not a type's: generating a type from that name would invent a class named
      // after the variable. Generate the variable's declared type instead.
      Type scopeVariableType =
          JavaParserUtil.getTypeIfMethodRefScopeNamesVariable(typeExpr, fqnsToCompilationUnits);

      inferContextImpl(scopeVariableType != null ? scopeVariableType : typeExpr.getType(), result);
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
      // The type being instantiated plays the role a receiver's type plays for a method call: it
      // is what supplies the declaring type's type arguments at this call site.
      FullyQualifiedNameSet instantiatedType;

      if (node instanceof ObjectCreationExpr constructor) {
        if (Resolver.calculateResolvedType(constructor) != null) {
          // If the type is resolvable, the constructor is too; a type in the constructor is not
          // solvable. Return because we don't need to generate a new constructor.
          return;
        }

        inferContextImpl(constructor.getType(), result);
        instantiatedType = fullyQualifiedNameGenerator.getFQNsFromType(constructor.getType());
        // Do not generate here; that should be taken care of in the inferContextImpl call above.
        scope = (UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(instantiatedType);

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

          if (Resolver.resolve(superClass) != null) {
            // If the type is resolvable, the constructor is too; a type in the constructor is not
            // solvable. Return because we don't need to generate a new constructor.
            return;
          }

          inferContextImpl(superClass, result);
          instantiatedType = fullyQualifiedNameGenerator.getFQNsFromType(superClass);
          // Do not generate here; that should be taken care of in the inferContextImpl call above.
          scope = (UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(instantiatedType);

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
          scope,
          JavaParserUtil.erase(constructorName),
          arguments,
          numberOfTypeParams,
          node,
          instantiatedType,
          result);
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
    ResolvedType resolved = Resolver.resolve(type);

    if (resolved == null) {
      Object resolvedAsObject = JavaParserUtil.tryResolveNodeIfInAnonymousClass(type);

      if (resolvedAsObject instanceof ResolvedType resolvedType) {
        resolved = resolvedType;
      }
    }

    if (resolved != null) {
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

        ResolvedType resolvedType = Resolver.resolve(typeArg);

        if (resolvedType != null && resolvedType.isTypeVariable()) {
          typeArgsPreferred.set(i, resolvedType.asTypeParameter().getName());
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

        StringBuilder signature = new StringBuilder(method.getName() + "(");
        List<Set<MemberType>> paramTypes = new ArrayList<>();

        for (int i = 0; i < method.getNumberOfParams(); i++) {
          signature.append(
              JavaParserUtil.getSimpleNameFromQualifiedName(
                  JavaParserUtil.erase(method.getParam(i).toString())));
          if (i < method.getNumberOfParams() - 1) {
            signature.append(", ");
          }

          paramTypes.add(Set.of(new SolvedMemberType(method.getParam(i).describeType())));
        }

        signature.append(")");

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

    if (Resolver.resolve(anno) != null) {
      return;
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

      if (toLookUpTypeFor.isAnnotationExpr()) {
        ResolvedAnnotationDeclaration resolved =
            Resolver.resolve(toLookUpTypeFor.asAnnotationExpr());
        if (resolved != null) {
          rawFqns = new FullyQualifiedNameSet(Set.of(resolved.getQualifiedName()));
        } else {
          rawFqns =
              fullyQualifiedNameGenerator
                  .getFQNsForExpressionType(toLookUpTypeFor)
                  .iterator()
                  .next();
        }
      } else if (toLookUpTypeFor instanceof FieldAccessExpr fieldAccess
          && JavaParserUtil.isProbablyAConstant(fieldAccess.getNameAsString())
          && !declaringTypeIsSyntheticNonEnum(fieldAccess)) {
        // An enum constant's type is the enum itself, so the member's type is the field's scope.
        rawFqns =
            fullyQualifiedNameGenerator
                .getFQNsForExpressionType(fieldAccess.getScope())
                .iterator()
                .next();
      } else {
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
    ResolvedValueDeclaration resolved = Resolver.resolve(field);
    if (resolved == null && Resolver.calculateResolvedType(field) != null) {
      // This is most likely a class; resolve() only works on field declarations.
      // System.out, for example, would fail to resolve() but calculateResolvedType() would work.
      return;
    }

    if (resolved != null) {
      Type type =
          JavaParserUtil.getTypeFromResolvedValueDeclaration(resolved, fqnsToCompilationUnits);

      if (type != null) {
        inferContextImpl(type, result);
      }

      return;
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

    Map<MemberType, NodeWithParameters<?>> typeToMustPreserveNode =
        getTypeToNodeWithParametersFromArgument(field);

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

      // Whether this field access is being generated as an enum constant, as opposed to a static
      // final constant of an ordinary class. Only meaningful when isInAnnotation is true.
      boolean isEnumConstant = false;

      if (isInAnnotation) {
        // A field access in an annotation argument is usually an enum constant, but it can also be
        // a static final constant of an ordinary class. This is only a guess, so it must not
        // override anything we actually know: a constructor call on the same type, for example,
        // establishes that it is a class (see the ObjectCreationExpr case of inferContextImpl).
        isEnumConstant = true;
        for (UnsolvedClassOrInterfaceAlternates parent : potentialParents) {
          if (parent.getType() == UnsolvedClassOrInterfaceType.UNKNOWN) {
            parent.setType(UnsolvedClassOrInterfaceType.ENUM);
          } else if (parent.getType() != UnsolvedClassOrInterfaceType.ENUM) {
            isEnumConstant = false;
          }
        }
      }

      boolean isStatic = JavaParserUtil.getFQNIfStaticMember(field) != null;

      UnsolvedFieldAlternates createdField;
      if (typeToMustPreserveNode.isEmpty()) {
        Set<MemberType> types;
        if (isEnumConstant) {
          // An enum constant declaration names no type, so the empty type is what should be
          // printed; see UnsolvedField#toString.
          types = Set.of(new SolvedMemberType(""));
        } else if (isInAnnotation) {
          // The declaring type is not an enum, so this is an ordinary field being used as an
          // annotation argument. JLS 9.7.1 requires such an argument to be a constant expression,
          // so the field must be given a primitive (or String) type and a constant initializer.
          types = Set.of(ANNOTATION_CONSTANT_TYPE);
        } else {
          types = new LinkedHashSet<>();
          for (FullyQualifiedNameSet potentialTypeFQNs :
              fullyQualifiedNameGenerator.getFQNsForExpressionType(field)) {
            types.add(getOrCreateMemberTypeFromFQNs(potentialTypeFQNs));
          }
        }

        createdField =
            UnsolvedFieldAlternates.create(
                field.getNameAsString(),
                types,
                potentialParents,
                // A constant expression must be reached through a static field, and a final field
                // that is not static would be emitted without an initializer.
                isStatic || (isInAnnotation && !isEnumConstant),
                // A non-enum constant used as an annotation argument must be final for it to be a
                // constant expression.
                isInAnnotation && !isEnumConstant);
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
    ResolvedValueDeclaration resolved = Resolver.resolve(nameExpr);
    if (resolved == null) {
      // If the declaration is not resolvable, then check to see if it is a
      // known class that has been passed in
      if (Resolver.calculateResolvedType(nameExpr) != null) {
        // This is most likely a class; resolve() only works on field/variable declarations.
        // System, for example, would fail to resolve() but calculateResolvedType() would work.
        return;
      }

      if (JavaParserUtil.tryResolveNodeIfInAnonymousClass(nameExpr) != null) {
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

    if (resolved != null) {
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
    }

    // An unqualified enum constant used as a switch case label is a member of the selector's enum
    // type, not a field of the enclosing class, and requires no import. Handle that special case.
    if (tryHandleSwitchEnumConstant(nameExpr, result)) {
      return;
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

    Map<MemberType, NodeWithParameters<?>> typeToMustPreserveNode =
        getTypeToNodeWithParametersFromArgument(nameExpr);

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
   * Handles the special case of an unqualified enum constant used as a switch case label. Java
   * permits an unqualified enum constant that is a member of the switch selector's type to be used
   * as a case label without an explicit import. Such a name is therefore a member of the (possibly
   * synthetic) enum used as the selector, not a field of the enclosing class.
   *
   * @param nameExpr the unsolved name expression
   * @param result the list of generated/found symbols to add to
   * @return true if nameExpr was handled as an enum constant (so no further handling is needed)
   */
  private boolean tryHandleSwitchEnumConstant(
      NameExpr nameExpr, List<UnsolvedSymbolAlternates<?>> result) {
    // Heuristic: enum constants follow the ALL_CAPS convention for constants. This avoids treating,
    // e.g., a call to a locally-scoped variable in a case label as an enum constant.
    if (!JavaParserUtil.isProbablyAConstant(nameExpr.getNameAsString())) {
      return false;
    }
    Expression selector = JavaParserUtil.getEnclosingSwitchSelectorIfCaseLabel(nameExpr);
    if (selector == null) {
      return false;
    }
    // Determine the (possibly synthetic) type of the switch selector.
    Set<FullyQualifiedNameSet> fqns =
        fullyQualifiedNameGenerator.getFQNsForExpressionType(selector);
    // TODO: should we take the possibility of multiple members of fqns here into account? I think
    // this will almost always return a set of size 0 or 1.
    Set<String> enumFQNs = fqns.isEmpty() ? null : fqns.iterator().next().erasedFqns();
    if (enumFQNs == null || enumFQNs.isEmpty() || doesOverlapWithKnownType(enumFQNs)) {
      // If the selector's type is a known type, it is not a synthetic enum that we control, so fall
      // back to the normal handling. (In practice, the constants of a known enum would already have
      // resolved above, so this only guards against unexpected inputs.)
      return false;
    }

    // Check to make sure we don't add a duplicate enum constant.
    Set<String> fieldFQNs =
        enumFQNs.stream()
            .map(fqn -> fqn + "#" + nameExpr.getNameAsString())
            .collect(Collectors.toSet());
    UnsolvedSymbolAlternates<?> existing = findExistingAndUpdateFQNs(fieldFQNs);
    if (existing instanceof UnsolvedFieldAlternates) {
      return true;
    }

    // Find or create the synthetic enum, mark it as an enum, and add this constant to it.
    UnsolvedClassOrInterfaceAlternates enumType =
        findExistingAndUpdateFQNsOrCreateNewType(enumFQNs);
    enumType.setType(UnsolvedClassOrInterfaceType.ENUM);

    // Enum constants are represented as (static, final) fields of the enum; only the name is used
    // when the declaring type is an enum, so the field's type is unimportant.
    UnsolvedFieldAlternates constant =
        UnsolvedFieldAlternates.create(
            nameExpr.getNameAsString(),
            getOrCreateMemberTypeFromFQNs(new FullyQualifiedNameSet(enumFQNs)),
            List.of(enumType),
            true,
            true);
    addNewSymbolToGeneratedSymbolsMap(constant);
    result.add(constant);
    return true;
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
    ResolvedMethodDeclaration resolvedMethodDeclaration = Resolver.resolve(methodCall);

    if (resolvedMethodDeclaration != null) {
      Node node =
          JavaParserUtil.tryFindAttachedNode(resolvedMethodDeclaration, fqnsToCompilationUnits);

      // Not every resolved method has a method AST: an enum's implicit values() and
      // valueOf(String) (JLS 8.9.3) report the enum declaration, which has no type to infer from.
      // Nothing is lost by skipping it, since their return types are the enum itself.
      if (node instanceof NodeWithType<?, ?> toAst) {
        inferContextImpl(toAst.getType(), result);
      }

      return;
    }

    List<? extends NodeWithParameters<?>> definitions =
        JavaParserUtil.tryResolveNodeWithUnresolvableArguments(methodCall, fqnsToCompilationUnits);

    if (!definitions.isEmpty()
        && methodCall.getArguments().stream()
            .allMatch(
                arg ->
                    Resolver.calculateResolvedType(arg) != null
                        || (arg instanceof Resolvable<?> r && Resolver.resolve(r) != null))) {
      // Special case: method declaration is findable, arguments are all solvable, but a parameter
      // type is not. In this case, the type of the parameters are unsolved, and should be preserved
      // if the parameter type ever ends up becoming used (which it will, after addInformation is
      // done).
      for (NodeWithParameters<?> callable : definitions) {
        for (Parameter param : callable.getParameters()) {
          List<UnsolvedSymbolAlternates<?>> generated = inferContext(param.getType());
          // Find the generated param type, if any
          for (UnsolvedSymbolAlternates<?> symbol : generated) {
            if (symbol instanceof UnsolvedClassOrInterfaceAlternates type) {
              if (type.getClassName().equals(JavaParserUtil.erase(param.getTypeAsString()))) {
                for (UnsolvedClassOrInterface alt : type.getAlternates()) {
                  alt.addMustPreserveNode((Node) callable);
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
    // inner map: original --> replaced
    Map<Expression, Map<FullyQualifiedNameSet, FullyQualifiedNameSet>>
        argumentToParameterPotentialFQNsWithMethodRefsHandled = new HashMap<>();

    Set<String> potentialFQNs =
        fullyQualifiedNameGenerator.generateMethodFQNsWithSideEffect(
            methodCall, potentialScopeFQNs, argumentToParameterPotentialFQNs, true);

    // For each method reference argument, we should replace the functional interface type arguments
    // with type parameters defined in this method in case this method generation is called
    // somewhere else with different wildcard bounds

    // This should be possible to handle even if the method call already has type arguments;
    // however, we don't have a good way to handle this yet, so we will just skip it for now.
    int totalNumberOfTypeParametersNeeded =
        handleMethodRefExprsInArgumentList(
            methodCall.getTypeArguments().isPresent()
                ? methodCall.getTypeArguments().get().size()
                : 0,
            methodCall.getArguments(),
            argumentToParameterPotentialFQNs,
            argumentToParameterPotentialFQNsWithMethodRefsHandled);

    // Type variables of the calling context can reach this signature through the argument and
    // return types, but are not in scope in the synthetic declaration, so the method has to declare
    // them itself.
    List<String> callerTypeVariablesToDeclare =
        typeVariablesToDeclareFromCaller(
            methodCall, typeVariableNamesSubstitutedAtCallSite(methodCall, potentialScopeFQNs));

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
                || fqns.stream()
                    .noneMatch(
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
    Map<MemberType, NodeWithParameters<?>> returnTypeToMustPreserveNode =
        getTypeToNodeWithParametersFromArgument(methodCall);

    List<Map<MemberType, @Nullable Node>> parametersToMustPreserve =
        generateParameterToMustPreserveMap(
            methodCall.getArguments(),
            argumentToParameterPotentialFQNs,
            argumentToParameterPotentialFQNsWithMethodRefsHandled);

    UnsolvedMethodAlternates generatedMethod;

    if (generated instanceof UnsolvedMethodAlternates) {
      generatedMethod = (UnsolvedMethodAlternates) generated;

      boolean returnTypeReplaced = false;

      if (!returnTypeToMustPreserveNode.isEmpty()) {
        Set<MemberType> oldReturnTypes = generatedMethod.getReturnTypes();
        generatedMethod.updateReturnTypesAndMustPreserveNodes(returnTypeToMustPreserveNode);

        // Handle the case where more information gives a single possible return type that is the
        // actual name of the return type, when all we knew earlier was a generated return type
        // name that was used as a placeholder.
        if (!generatedMethod.getReturnTypes().equals(oldReturnTypes)
            && oldReturnTypes.size() == 1
            && oldReturnTypes.iterator().next() instanceof UnsolvedMemberType unsolved
            && unsolved.usesGeneratedName()
            && generatedMethod.getReturnTypes().size() == 1) {
          removeTypeAndReplaceUses(unsolved, generatedMethod.getReturnTypes().iterator().next());
          returnTypeReplaced = true;
        }
      } else if (generatedMethod.getReturnTypes().size() == 1
          && generatedMethod.getReturnTypes().iterator().next()
              instanceof UnsolvedMemberType unsolved
          && unsolved.usesGeneratedName()) {
        Set<FullyQualifiedNameSet> returnTypeFQNs =
            fullyQualifiedNameGenerator.getFQNsForExpressionType(methodCall);
        Set<MemberType> returnTypes =
            returnTypeFQNs.stream()
                .map(this::getOrCreateMemberTypeFromFQNs)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (!returnTypeFQNs.isEmpty() && !returnTypeFQNs.iterator().next().usesGeneratedName()) {
          generatedMethod.replaceReturnType(unsolved, returnTypes);
          removeTypeAndReplaceUses(unsolved, returnTypes.toArray(MemberType[]::new));
          returnTypeReplaced = true;
        }
      }

      // Handle these cases when the generated and new differ:
      // 1: same erasure, but different type argument value(s) in parameter list --> use
      // unconstrained type variables for the type variable
      // 2: #1 and different return type (or same erasure but different type argument value(s)) -->
      // use unconstrained type variable for the return type (match the type variable in the
      // parameter list if it is the same type variable)

      // Case 1
      Set<MemberType> typesInOriginalToReplaceWithTypeVariables = new LinkedHashSet<>();
      for (List<MemberType> paramList :
          JavaParserUtil.generateAllCombinations(generatedMethod.getParameterList())) {
        for (int i = 0; i < paramList.size(); i++) {
          MemberType generatedType = paramList.get(i);
          Set<MemberType> newTypes = parametersToMustPreserve.get(i).keySet();

          for (MemberType newType : newTypes) {
            if (!generatedType.getFullyQualifiedNames().equals(newType.getFullyQualifiedNames())) {
              continue;
            }

            Map<MemberType, MemberType> differences =
                SpeciminGenerationUtils.getDifferentMemberTypes(generatedType, newType);

            if (differences.isEmpty()) {
              continue;
            }

            typesInOriginalToReplaceWithTypeVariables.addAll(
                differences.keySet().stream()
                    .filter(t -> !SpeciminGenerationUtils.isATypeVariable(t))
                    .collect(Collectors.toSet()));
          }
        }
      }

      Map<MemberType, MemberType> originalToReplacement = new HashMap<>();
      if (methodCall.getTypeArguments().isPresent()
          && !methodCall.getTypeArguments().get().isEmpty()) {
        generatedMethod.setNumberOfTypeVariables(methodCall.getTypeArguments().get().size());

        for (int i = 0; i < methodCall.getTypeArguments().get().size(); i++) {
          Type typeArg = methodCall.getTypeArguments().get().get(i);

          FullyQualifiedNameSet fqn = fullyQualifiedNameGenerator.getFQNsFromType(typeArg);
          MemberType memberTypeForTypeArg = getMemberTypeFromFQNs(fqn, false);

          if (memberTypeForTypeArg == null
              || !typesInOriginalToReplaceWithTypeVariables.contains(memberTypeForTypeArg)) {
            continue;
          }

          originalToReplacement.put(
              memberTypeForTypeArg, new SolvedMemberType(generatedMethod.getTypeVariableName(i)));
        }
      } else {
        int typeVar = generatedMethod.getNumberOfTypeVariables();
        generatedMethod.setNumberOfTypeVariables(
            generatedMethod.getNumberOfTypeVariables()
                + typesInOriginalToReplaceWithTypeVariables.size());

        for (MemberType typeToReplace : typesInOriginalToReplaceWithTypeVariables) {
          MemberType newTypeVariable =
              new SolvedMemberType(generatedMethod.getTypeVariableName(typeVar));

          if (typeToReplace.equals(newTypeVariable)) {
            continue;
          }

          originalToReplacement.put(typeToReplace, newTypeVariable);

          typeVar++;
        }
      }

      for (Map.Entry<MemberType, MemberType> entry : originalToReplacement.entrySet()) {
        for (MemberType param :
            generatedMethod.getParameterList().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toSet())) {
          MemberType newParamType =
              SpeciminGenerationUtils.copyTypeWithReplacedMemberType(
                  param, entry.getKey(), entry.getValue());
          generatedMethod.replaceParameterType(param, Set.of(newParamType));
        }
      }

      // Case 2
      boolean before = fullyQualifiedNameGenerator.getShouldCheckGeneratedSymbols();
      fullyQualifiedNameGenerator.setShouldCheckGeneratedSymbols(false);
      Set<FullyQualifiedNameSet> returnTypeFQNs =
          fullyQualifiedNameGenerator.getFQNsForExpressionType(methodCall);
      fullyQualifiedNameGenerator.setShouldCheckGeneratedSymbols(before);

      Set<MemberType> returnTypes =
          returnTypeFQNs.stream()
              .filter(fqnSet -> !fqnSet.usesGeneratedName())
              .map(this::getOrCreateMemberTypeFromFQNs)
              .collect(Collectors.toCollection(LinkedHashSet::new));

      Set<MemberType> generatedMethodReturnTypes = generatedMethod.getReturnTypes();
      if (!returnTypes.isEmpty() && !generatedMethodReturnTypes.equals(returnTypes)) {
        if (generatedMethod.getNumberOfTypeVariables() > 0
            && generatedMethodReturnTypes.size() == 1
            && generatedMethodReturnTypes.iterator().next() instanceof UnsolvedMemberType unsolved
            && unsolved.usesGeneratedName()) {
          Set<MemberType> potentialReturns = new LinkedHashSet<>();
          for (int i = 0; i < generatedMethod.getNumberOfTypeVariables(); i++) {
            potentialReturns.add(new SolvedMemberType(generatedMethod.getTypeVariableName(i)));
          }

          generatedMethod.replaceReturnType(unsolved, potentialReturns);

          // If we don't know the return type, we will just use Object as a placeholder
          // so the output still compiles.

          // This can be the case with foo(mock(...)):
          // If mock is discovered to be mock(T) returns T, and foo was already generated with
          // foo(MockReturnType), then we replace MockReturnType with Object so the output is
          // compilable. Perhaps it is better to make foo also use a type variable; but these
          // cases should be rare anyway.
          removeTypeAndReplaceUses(unsolved, SolvedMemberType.JAVA_LANG_OBJECT);
          returnTypeReplaced = true;
        } else if (!originalToReplacement.isEmpty()
            && !(generatedMethodReturnTypes.size() == 1
                && SpeciminGenerationUtils.isATypeVariable(
                    generatedMethodReturnTypes.iterator().next()))) {
          for (MemberType returnType : generatedMethodReturnTypes) {
            MemberType replaceWith = originalToReplacement.get(returnType);

            if (replaceWith == null) {
              continue;
            }

            generatedMethod.replaceReturnType(returnType, replaceWith);
            returnTypeReplaced = true;
          }
        }
      }

      if (returnTypeReplaced) {
        // Special case: since we now know the method's return type, we may need to update its
        // parent/child nodes since they may have used outdated type information

        // For example, if this an unsolved method as an argument to this method call was
        // already generated with a generated return type as its type, and we now know
        // what the actual return type should be, we need to update it now in case we
        // do not encounter that inner method again.

        // We also find all instances of the method and method calls with methodCall
        // in its scope, which guarantees we find most cases where the method call is used.
        // This is relatively expensive, but ok since this shouldn't need to run in most cases.

        // Ignore generated symbols so we can get updated types
        before = fullyQualifiedNameGenerator.getShouldCheckGeneratedSymbols();
        fullyQualifiedNameGenerator.setShouldCheckGeneratedSymbols(false);
        for (MethodCallExpr methodToUpdate :
            methodCall
                .findCompilationUnit()
                .get()
                .findAll(
                    MethodCallExpr.class,
                    m -> m.toString().contains(methodCall.getNameAsString()))) {
          Node parentNode = methodToUpdate;
          while (parentNode != null) {
            inferContextImpl(parentNode, result);
            parentNode = parentNode.getParentNode().orElse(null);
          }

          for (Node argument : methodToUpdate.getArguments()) {
            inferContextImpl(argument, result);
          }
        }
        fullyQualifiedNameGenerator.setShouldCheckGeneratedSymbols(before);
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

      generatedMethod.setNumberOfTypeVariables(totalNumberOfTypeParametersNeeded);
      generatedMethod.declareTypeVariables(callerTypeVariablesToDeclare);

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
   * @param callSite The constructor call, used to find the calling context's type variables
   * @param instantiatedType The type being instantiated, which supplies the declaring type's type
   *     arguments at this call site
   * @param result The result of inferContext
   */
  private void handleConstructorCall(
      UnsolvedClassOrInterfaceAlternates location,
      String constructorName,
      List<Expression> arguments,
      int numberOfTypeParams,
      Node callSite,
      FullyQualifiedNameSet instantiatedType,
      List<UnsolvedSymbolAlternates<?>> result) {
    Map<Expression, Set<FullyQualifiedNameSet>> argumentToParameterPotentialFQNs = new HashMap<>();

    // inner map: original --> replaced
    Map<Expression, Map<FullyQualifiedNameSet, FullyQualifiedNameSet>>
        argumentToParameterPotentialFQNsWithMethodRefsHandled = new HashMap<>();

    // For each method reference argument, we should replace the functional interface type arguments
    // with type parameters defined in this method in case this method generation is called
    // somewhere else with different wildcard bounds

    // This should be possible to handle even if the method call already has type arguments;
    // however, we don't have a good way to handle this yet, so we will just skip it for now.
    numberOfTypeParams =
        handleMethodRefExprsInArgumentList(
            numberOfTypeParams, arguments,
            argumentToParameterPotentialFQNs,
                argumentToParameterPotentialFQNsWithMethodRefsHandled);
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

    // Type variables of the calling context can reach this signature through the argument types,
    // but are not in scope in the synthetic declaration, so the constructor has to declare them
    // itself.
    List<String> callerTypeVariablesToDeclare =
        typeVariablesToDeclareFromCaller(
            callSite, typeVariableNamesSubstitutedByReceiver(instantiatedType));

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

    if (!(generated instanceof UnsolvedMethodAlternates)) {
      for (Expression argument : arguments) {
        inferContextImpl(argument, result);
      }

      List<Map<MemberType, @Nullable Node>> parametersToMustPreserve =
          generateParameterToMustPreserveMap(
              arguments,
              argumentToParameterPotentialFQNs,
              argumentToParameterPotentialFQNsWithMethodRefsHandled);

      UnsolvedMethodAlternates generatedMethod =
          UnsolvedMethodAlternates.createWithPreservation(
              constructorName,
              Set.of(new SolvedMemberType("")),
              List.of(location),
              parametersToMustPreserve,
              List.of());

      addNewSymbolToGeneratedSymbolsMap(generatedMethod);

      generatedMethod.setNumberOfTypeVariables(numberOfTypeParams);
      generatedMethod.declareTypeVariables(callerTypeVariablesToDeclare);

      result.add(generatedMethod);
    }
  }

  /**
   * Returns the type variable names that the parameterization at this call site substitutes into
   * the declaring type's type parameters, and which a member generated here may therefore keep
   * referring to. See {@link #typeVariableNamesSubstitutedByReceiver} for why that is the
   * criterion.
   *
   * @param methodCall the call site
   * @param potentialScopeFQNs the types that could declare this method
   * @return the substituted type variable names, or an empty set if there is no such
   *     parameterization
   */
  private Set<String> typeVariableNamesSubstitutedAtCallSite(
      MethodCallExpr methodCall, Collection<Set<String>> potentialScopeFQNs) {
    if (methodCall.hasScope()
        && !methodCall.getScope().get().isThisExpr()
        && !methodCall.getScope().get().isSuperExpr()) {
      Set<FullyQualifiedNameSet> scopeTypes =
          fullyQualifiedNameGenerator.getFQNsForExpressionType(methodCall.getScope().get());

      // With several possibilities there is no single parameterization to read a substitution off
      // of, so report nothing and let every caller type variable be declared on the member; that is
      // always compilable, only less precise.
      return scopeTypes.size() == 1
          ? typeVariableNamesSubstitutedByReceiver(scopeTypes.iterator().next())
          : Set.of();
    }

    // For this, super, and unqualified calls the member is declared in an ancestor, and the
    // parameterization that JLS 4.5.2 substitutes through is the one written in this type's extends
    // or implements clause.
    TypeDeclaration<?> enclosing = JavaParserUtil.getEnclosingClassLikeOptional(methodCall);

    if (enclosing == null || !enclosing.isClassOrInterfaceDeclaration()) {
      return Set.of();
    }

    ClassOrInterfaceDeclaration enclosingClass = enclosing.asClassOrInterfaceDeclaration();
    List<ClassOrInterfaceType> ancestors = new ArrayList<>(enclosingClass.getExtendedTypes());
    ancestors.addAll(enclosingClass.getImplementedTypes());

    Set<String> substituted = new LinkedHashSet<>();
    for (ClassOrInterfaceType ancestor : ancestors) {
      FullyQualifiedNameSet ancestorFQNs = fullyQualifiedNameGenerator.getFQNsFromType(ancestor);

      // Only ancestors that could declare this method: another ancestor's parameterization says
      // nothing about the type this method actually lands in.
      if (potentialScopeFQNs.stream()
          .noneMatch(scope -> !Collections.disjoint(scope, ancestorFQNs.erasedFqns()))) {
        continue;
      }

      substituted.addAll(typeVariableNamesSubstitutedByReceiver(ancestorFQNs));
    }
    return substituted;
  }

  /**
   * Returns the type variables of the calling context that a synthetic member generated from this
   * call site must declare itself.
   *
   * <p>A synthetic member's parameter and return types are built out of the types of the
   * expressions at a call site, and those types may mention type variables bound by the calling
   * method or class. Such a name means nothing in the synthetic declaration (JLS 6.3): depending on
   * what happens to be declared there it is either unresolvable or, worse, silently captured by an
   * unrelated declaration of the same name. Declaring it on the member binds it, and costs no
   * precision, because invocation type inference (JLS 18.5) instantiates it back to the caller's
   * type variable at this call site.
   *
   * <p>The exception is a name that reaches the signature through the declaring type's own type
   * parameter, which {@code substitutedByReceiver} identifies; see {@link
   * #typeVariableNamesSubstitutedByReceiver}. Declaring such a name on the member would shadow the
   * declaring type's type parameter and sever that substitution.
   *
   * <p>Names that no signature ends up mentioning are harmless to include, since {@link
   * UnsolvedMethod} prints only the type variables its signature actually uses.
   *
   * @param callSite the call site the signature is being derived from
   * @param substitutedByReceiver names that must not be declared, because the receiver substitutes
   *     them into the declaring type's type parameters
   * @return the type variable names to declare, innermost enclosing declaration first
   */
  private List<String> typeVariablesToDeclareFromCaller(
      Node callSite, Set<String> substitutedByReceiver) {
    List<String> toDeclare = new ArrayList<>();
    for (String name : JavaParserUtil.getReferenceableTypeParameterNames(callSite)) {
      if (!substitutedByReceiver.contains(name)) {
        toDeclare.add(name);
      }
    }
    return toDeclare;
  }

  /**
   * Returns the type variable names that a synthetic member may keep referring to because the
   * receiver at the call site substitutes them into its declaring type's type parameters.
   *
   * <p>By JLS 4.5.2 the type of a member of {@code C<A1..An>} is its type in {@code C} with the
   * class's type parameters replaced by {@code A1..An}. So a name written at position {@code i} of
   * the signature denotes {@code Ai} at this call site, and keeping the caller's name there is
   * correct exactly when the class's {@code i}th type parameter is spelled the same as the caller's
   * type variable and the receiver instantiates it to that very type variable. Any other case --
   * notably a static member, where JLS 8.1.2 forbids referring to the class's type parameters at
   * all, and a receiver that instantiates the parameter to something else -- has no such channel.
   *
   * <p>A member accessed through a type name rather than an instance needs no special case: the
   * type of such a scope carries no type arguments, so nothing is reported as substitutable.
   *
   * @param receiverType the receiver's type at the call site, or the instantiated type for a
   *     constructor; null if there is no receiver
   * @return the type variable names that the receiver substitutes into the declaring type
   */
  private Set<String> typeVariableNamesSubstitutedByReceiver(
      @Nullable FullyQualifiedNameSet receiverType) {
    if (receiverType == null || receiverType.typeArguments().isEmpty()) {
      return Set.of();
    }

    if (!(findExistingAndUpdateFQNs(receiverType.erasedFqns())
        instanceof UnsolvedClassOrInterfaceAlternates declaringType)) {
      return Set.of();
    }

    List<String> classTypeParams = declaringType.getTypeVariables();
    List<FullyQualifiedNameSet> typeArgs = receiverType.typeArguments();

    Set<String> substituted = new LinkedHashSet<>();
    for (int i = 0; i < typeArgs.size(); i++) {
      String typeArgName = getTypeVariableNameIfBare(typeArgs.get(i));

      if (typeArgName == null) {
        continue;
      }

      // The declaring type may not have been given its type parameter names yet. When that is so,
      // this parameterization is the one that will name them, since handleClassOrInterfaceType
      // prefers the call site's type variable names and the first parameterization to arrive wins.
      if (classTypeParams.isEmpty()
          || (i < classTypeParams.size() && typeArgName.equals(classTypeParams.get(i)))) {
        substituted.add(typeArgName);
      }
    }
    return substituted;
  }

  /**
   * Returns the name this FQN set holds if it is a bare name -- one with no package, no type
   * arguments and no wildcard, which is how {@link FullyQualifiedNameGenerator} represents a type
   * variable -- and null otherwise.
   *
   * @param fqnSet the FQN set to inspect
   * @return the bare name, or null if this FQN set does not hold one
   */
  private static @Nullable String getTypeVariableNameIfBare(FullyQualifiedNameSet fqnSet) {
    if (fqnSet.wildcard() != null
        || !fqnSet.typeArguments().isEmpty()
        || fqnSet.erasedFqns().size() != 1) {
      return null;
    }

    String name = fqnSet.erasedFqns().iterator().next();
    if (name.indexOf('.') >= 0 || name.indexOf('[') >= 0 || JavaLangUtils.isPrimitive(name)) {
      return null;
    }
    return name;
  }

  /**
   * For each method reference argument, we should replace the functional interface type arguments
   * with type parameters defined in this method in case this method generation is called somewhere
   * else with different wildcard bounds
   *
   * <p>This method currently does not support method calls that already have type arguments, but
   * this should be added in the future.
   *
   * @param existingTypeParams The number of type parameters already defined in the method
   * @param arguments The list of argument expressions to check for method references
   * @param argumentToParameterPotentialFQNs A map from each argument expression to its potential
   *     fully qualified name sets (modified by side effect)
   * @param argumentToParameterPotentialFQNsWithMethodRefsHandled A map from each argument
   *     expression to a map from original fully qualified name sets to modified fully qualified
   *     name sets, used for method references. (modified by side effect)
   * @return The updated number of type parameters after handling method reference arguments
   */
  private int handleMethodRefExprsInArgumentList(
      int existingTypeParams,
      List<Expression> arguments,
      Map<Expression, Set<FullyQualifiedNameSet>> argumentToParameterPotentialFQNs,
      Map<Expression, Map<FullyQualifiedNameSet, FullyQualifiedNameSet>>
          argumentToParameterPotentialFQNsWithMethodRefsHandled) {
    if (existingTypeParams > 0) {
      return existingTypeParams;
    }
    for (Expression argument : arguments) {
      if (argument.isMethodReferenceExpr()) {
        Set<FullyQualifiedNameSet> potentialFQNsForArgument =
            argumentToParameterPotentialFQNs.get(argument);

        if (potentialFQNsForArgument == null) {
          // Impossible: satisfy null checker
          throw new RuntimeException(
              "Potential FQNs for argument should have been generated for method reference"
                  + " argument: "
                  + argument);
        }

        Map<FullyQualifiedNameSet, FullyQualifiedNameSet> potentialFQNsForArgumentWithTypeArgs =
            new LinkedHashMap<>();
        // Use the maximum number of type parameters. Doesn't matter if we have extra since
        // UnsolvedMethod handles it.
        int maxNumberOfTypeParameters = 0;
        for (FullyQualifiedNameSet fqnSet : potentialFQNsForArgument) {
          // Don't worry about nested type arguments since these should all be functional
          // interfaces
          maxNumberOfTypeParameters =
              Math.max(maxNumberOfTypeParameters, fqnSet.typeArguments().size());

          List<FullyQualifiedNameSet> typeArgumentsWithTypeParams = new ArrayList<>();
          for (int i = 0; i < fqnSet.typeArguments().size(); i++) {
            typeArgumentsWithTypeParams.add(
                new FullyQualifiedNameSet(
                    JavaParserUtil.getGeneratedTypeParameterName(existingTypeParams + i)));
          }

          potentialFQNsForArgumentWithTypeArgs.put(
              fqnSet,
              new FullyQualifiedNameSet(
                  fqnSet.erasedFqns(),
                  typeArgumentsWithTypeParams,
                  null,
                  fqnSet.usesGeneratedName()));

          if (argumentToParameterPotentialFQNsWithMethodRefsHandled == null) {
            argumentToParameterPotentialFQNsWithMethodRefsHandled = new HashMap<>();
          }

          argumentToParameterPotentialFQNsWithMethodRefsHandled.put(
              argument, potentialFQNsForArgumentWithTypeArgs);
          argumentToParameterPotentialFQNs.put(
              argument, new LinkedHashSet<>(potentialFQNsForArgumentWithTypeArgs.values()));
        }

        existingTypeParams += maxNumberOfTypeParameters;
      }
    }

    return existingTypeParams;
  }

  /**
   * Given a list of argument expressions (from a method call, constructor call) and a map of
   * arguments to potential FQN sets, return a list of maps, each representing mutually exclusive
   * parameter types to nodes that must be preserved if that parameter type is used.
   *
   * @param args The collection of argument expressions
   * @param argumentToParameterPotentialFQNs A map from each argument expression to its potential
   *     fully qualified name sets
   * @param originalToModifiedParameterPotentialFQNs A map of arguments to a map from original fully
   *     qualified name sets to modified fully qualified name sets, used for method references.
   * @return A list of maps, each representing mutually exclusive parameter types to nodes that must
   *     be preserved
   */
  private List<Map<MemberType, @Nullable Node>> generateParameterToMustPreserveMap(
      Collection<Expression> args,
      Map<Expression, Set<FullyQualifiedNameSet>> argumentToParameterPotentialFQNs,
      Map<Expression, Map<FullyQualifiedNameSet, FullyQualifiedNameSet>>
          originalToModifiedParameterPotentialFQNs) {
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
          if (!(JavaParserUtil.tryFindAttachedNode(method, fqnsToCompilationUnits)
              instanceof NodeWithParameters<?> ast)) {
            continue;
          }

          FullyQualifiedNameSet potentialParameterType =
              fullyQualifiedNameGenerator.getFunctionalInterfaceForResolvedMethod(
                  argument.asMethodReferenceExpr(), method);

          Map<FullyQualifiedNameSet, FullyQualifiedNameSet> modifiedFQNs =
              originalToModifiedParameterPotentialFQNs.get(argument);
          if (modifiedFQNs != null && modifiedFQNs.containsKey(potentialParameterType)) {
            potentialParameterType = modifiedFQNs.get(potentialParameterType);
          }

          potentialParameterToMustPreserveNode.put(potentialParameterType, (Node) ast);
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
    Map<ClassOrInterfaceType, Set<String>> potentialScopeFQNs;
    if (methodDecl.getParentNode().orElse(null) instanceof ObjectCreationExpr anonClass) {
      ResolvedType resolvedType = Resolver.resolve(anonClass.getType());
      if (resolvedType != null) {
        TypeDeclaration<?> parentClass =
            JavaParserUtil.getTypeFromQualifiedName(
                resolvedType.describe(), fqnsToCompilationUnits);

        if (parentClass == null) {
          return;
        }

        potentialScopeFQNs =
            fullyQualifiedNameGenerator.getFQNsOfAllUnresolvableParents(parentClass, methodDecl);
      } else {
        potentialScopeFQNs =
            Map.of(
                anonClass.getType(),
                fullyQualifiedNameGenerator.getFQNsFromType(anonClass.getType()).erasedFqns());
      }
    } else {
      potentialScopeFQNs =
          fullyQualifiedNameGenerator.getFQNsOfAllUnresolvableParents(
              JavaParserUtil.getEnclosingClassLike(methodDecl), methodDecl);
    }

    if (potentialScopeFQNs.isEmpty()) {
      // If there are no potential scope FQNs, then this method is likely an override of a method
      // in an existing class or JDK interface
      return;
    }

    List<UnsolvedClassOrInterfaceAlternates> potentialDeclaringTypes = new ArrayList<>();

    for (Set<String> fqns : potentialScopeFQNs.values()) {
      potentialDeclaringTypes.add(findExistingAndUpdateFQNsOrCreateNewType(fqns));
    }

    List<Set<MemberType>> parameters = new ArrayList<>();

    // Includes "dirty" parameters; i.e., may include the wrong type variable.
    // This is ONLY to match previously-generated methods that may have used
    // those wrong type variables, and we will use this opportunity to update
    // that definition.
    boolean hasDirty = false;
    List<MemberType> dirtyParameters = new ArrayList<>();
    for (Parameter param : methodDecl.getParameters()) {
      MemberType paramType =
          getOrCreateMemberTypeFromFQNs(
              fullyQualifiedNameGenerator.getFQNsFromType(param.getType()));

      Set<MemberType> replacedParamTypes = new LinkedHashSet<>();
      for (ClassOrInterfaceType declaringType : potentialScopeFQNs.keySet()) {
        MemberType replacedParamType =
            replaceMethodTypeTypeArgumentsWithActual(
                paramType,
                (ClassOrInterfaceDeclaration) JavaParserUtil.getEnclosingClassLike(methodDecl),
                declaringType,
                findExistingAndUpdateFQNsOrCreateNewType(potentialScopeFQNs.get(declaringType)));

        if (replacedParamType == null) {
          continue;
        }

        replacedParamTypes.add(replacedParamType);
      }

      // Here, we add the "dirty" parameter type
      dirtyParameters.add(paramType);

      if (replacedParamTypes.isEmpty()) {
        replacedParamTypes.add(paramType);
      } else {
        hasDirty = true;
      }

      parameters.add(replacedParamTypes);
    }

    Set<String> potentialMethodFQNs = new LinkedHashSet<>();
    for (List<MemberType> paramList : JavaParserUtil.generateAllCombinations(parameters)) {
      for (Set<String> set : potentialScopeFQNs.values()) {
        for (String potentialScopeFQN : set) {
          potentialMethodFQNs.add(
              potentialScopeFQN
                  + "#"
                  + methodDecl.getNameAsString()
                  + "("
                  + String.join(
                      ", ",
                      paramList.stream()
                          .map(
                              p ->
                                  JavaParserUtil.getSimpleNameFromQualifiedName(
                                      JavaParserUtil.erase(p.toString())))
                          .toList())
                  + ")");
        }
      }
    }

    // Ensure all the scope types are generated (including type parameters)
    for (ClassOrInterfaceType scopeType : potentialScopeFQNs.keySet()) {
      inferContextImpl(scopeType, result);
    }

    MemberType returnType =
        getOrCreateMemberTypeFromFQNs(
            fullyQualifiedNameGenerator.getFQNsFromType(methodDecl.getType()));

    // If returnType contains any type parameter usages, they will not be available in
    // the generated method, so we must replace them with the type parameters available
    // in the generated method's declaring type. Since getFQNsFromType always returns
    // FQNs for reference types, we can safely assume that other non-primitive types
    // are those type parameter usages.

    Set<MemberType> replacedReturnTypes = new LinkedHashSet<>();
    for (ClassOrInterfaceType declaringType : potentialScopeFQNs.keySet()) {
      MemberType replacedReturnType =
          replaceMethodTypeTypeArgumentsWithActual(
              returnType,
              (ClassOrInterfaceDeclaration) JavaParserUtil.getEnclosingClassLike(methodDecl),
              declaringType,
              findExistingAndUpdateFQNsOrCreateNewType(potentialScopeFQNs.get(declaringType)));

      if (replacedReturnType == null) {
        continue;
      }

      replacedReturnTypes.add(replacedReturnType);
    }

    if (hasDirty) {
      Set<String> dirtyMethodFQNs = new LinkedHashSet<>();
      for (Set<String> set : potentialScopeFQNs.values()) {
        for (String potentialScopeFQN : set) {
          dirtyMethodFQNs.add(
              potentialScopeFQN
                  + "#"
                  + methodDecl.getNameAsString()
                  + "("
                  + String.join(
                      ", ",
                      dirtyParameters.stream()
                          .map(
                              p ->
                                  JavaParserUtil.getSimpleNameFromQualifiedName(
                                      JavaParserUtil.erase(p.toString())))
                          .toList())
                  + ")");
        }
      }

      UnsolvedMethodAlternates dirtyGenerated =
          (UnsolvedMethodAlternates) findExistingAndUpdateFQNs(dirtyMethodFQNs);

      if (dirtyGenerated != null) {
        removeSymbolFromGeneratedSymbolsMap(dirtyGenerated);

        List<Set<MemberType>> oldParameters = dirtyGenerated.getParameterList();
        for (int i = 0; i < oldParameters.size(); i++) {
          for (MemberType oldParam : oldParameters.get(i)) {
            dirtyGenerated.replaceParameterType(oldParam, parameters.get(i));
          }
        }

        if (dirtyGenerated.getReturnTypes().size() == 1) {
          MemberType oldReturnType = dirtyGenerated.getReturnTypes().iterator().next();
          dirtyGenerated.replaceReturnType(
              oldReturnType,
              replacedReturnTypes.isEmpty() ? Set.of(returnType) : replacedReturnTypes);
        }

        addNewSymbolToGeneratedSymbolsMap(dirtyGenerated);
        return;
      }
    }

    UnsolvedMethodAlternates generated =
        (UnsolvedMethodAlternates) findExistingAndUpdateFQNs(potentialMethodFQNs);

    if (generated != null) {
      if (generated.getReturnTypes().size() == 1 && !replacedReturnTypes.isEmpty()) {
        // Only replace return type if we have a single return type and when we have new
        // return types (type variables fixed) to replace with. The original return type is
        // likely to be an incorrect type variable if replacedReturnTypes is not empty.
        generated.replaceReturnType(
            generated.getReturnTypes().iterator().next(), replacedReturnTypes);
      }
      return;
    }

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

    // Ideally, we'd have alternates generate with pairs of the proper declaring type to
    // the proper return type, but this is currently a lot of work since each alternate
    // does not contain information about the declaring type. Since the cases where this method
    // is called AND where there are multiple possible declaring types are rare, we can just
    // generate them all for now.
    generated =
        UnsolvedMethodAlternates.create(
            methodDecl.getNameAsString(),
            replacedReturnTypes.isEmpty() ? Set.of(returnType) : replacedReturnTypes,
            potentialDeclaringTypes,
            parameters,
            exceptions,
            accessModifier);

    addNewSymbolToGeneratedSymbolsMap(generated);
    result.add(generated);
  }

  /**
   * Given an unsolved method type (either return or parameter type) and a known type, replace any
   * type variables that may be valid in the known type but invalid in the declaring type of the
   * unsolved method.
   *
   * @param type The unsolved method type
   * @param currentType The type where the known version of the method is declared (typically the
   *     class holding a known super method)
   * @param generateIn The unsolved type where the unsolved method should be generated
   * @param declaringType The declaring type corresponding with generateIn
   * @return The member type with type variables replaced, or null if the operation could not be
   *     completed
   */
  private @Nullable MemberType replaceMethodTypeTypeArgumentsWithActual(
      MemberType type,
      ClassOrInterfaceDeclaration currentType,
      ClassOrInterfaceType generateIn,
      UnsolvedClassOrInterfaceAlternates declaringType) {
    ClassOrInterfaceDeclaration generateInDecl =
        (ClassOrInterfaceDeclaration) JavaParserUtil.getEnclosingClassLike(generateIn);

    // For the enclosing class of generateIn
    Map<String, String> typeParamMapping =
        JavaParserUtil.generateTypeParameterMap(currentType, generateInDecl);

    // Now, we need to compose the above map onto the type parameters of the declaringType
    Map<String, String> finalTypeParamMapping = new HashMap<>();

    Optional<NodeList<Type>> typeArgs = generateIn.getTypeArguments();

    if (typeArgs.isEmpty()) {
      return null;
    }

    for (Map.Entry<String, String> entry : typeParamMapping.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      for (int i = 0; i < typeArgs.get().size(); i++) {
        Type typeArg = typeArgs.get().get(i);

        if (typeArg.toString().equals(value)) {
          finalTypeParamMapping.put(key, declaringType.getTypeVariables().get(i));
        }
      }
    }

    // Now, we need to replace the type arguments in returnType with the actual types from
    // finalTypeParamMapping. Since JavaParserUtil#generateTypeParameterMap currently only
    // works with solvable types (we should probably fix this in the future so it works
    // with unsolved types), we can just use SolvedMemberType here

    return replaceAllTypeArgumentsWith(
        type,
        finalTypeParamMapping.entrySet().stream()
            .collect(
                Collectors.toMap(
                    e -> new SolvedMemberType(e.getKey()),
                    e -> new SolvedMemberType(e.getValue()))));
  }

  /**
   * Helper method for {@link #replaceMethodTypeTypeArgumentsWithActual(MemberType,
   * ClassOrInterfaceDeclaration, ClassOrInterfaceType, UnsolvedClassOrInterfaceAlternates)}.
   * Recursively replaces all type arguments in a MemberType with the corresponding types in the
   * replacement map.
   *
   * @param type The MemberType to replace type arguments in
   * @param replacementMap A map of type arguments to their replacements
   * @return A new MemberType with all type arguments replaced
   */
  private MemberType replaceAllTypeArgumentsWith(
      MemberType type, Map<MemberType, MemberType> replacementMap) {
    if (replacementMap.containsKey(type)) {
      return replacementMap.get(type);
    }

    List<MemberType> newTypeArguments = new ArrayList<>();
    for (MemberType arg : type.getTypeArguments()) {
      newTypeArguments.add(replaceAllTypeArgumentsWith(arg, replacementMap));
    }

    return type.copyWithNewTypeArgs(newTypeArguments);
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

      // A constructor is never static, and the scope of a constructor reference is never a
      // receiver: the functional interface's parameters are the constructor's, one for one.
      if (!isConstructor && JavaParserUtil.methodRefHasTypeScope(methodRef)) {
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
      isVoid = JavaParserUtil.isVoidBlockBodyLambda(lambda);
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
   * result of {@link FullyQualifiedNameGenerator#getFQNsForExpressionLocation(Expression)} to this
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
   * NodeWithParameters. For example, if the argument is a method call expression, foo(), as an
   * argument of another method call, bar(foo()), this method will return a map of potential return
   * types of foo() (based on the definitions of bar with an arity of 1) to the NodeWithParameters
   * of bar. This method also works if {@code argument} is a field expression, or if the parent node
   * is a constructor/explicit constructor invocation.
   *
   * @param argument The argument expression to analyze
   * @return A map of potential return types to their corresponding NodeWithParameters. Returns an
   *     empty map if no potential return types are found, or if the argument is not part of a
   *     solvable method/constructor call.
   */
  private Map<MemberType, NodeWithParameters<?>> getTypeToNodeWithParametersFromArgument(
      Expression argument) {
    // If this expression is an argument of a solvable method call, we have multiple potential field
    // types to choose from, based on each definition
    Node parent = argument.getParentNode().get();
    int paramNum = -1;
    Map<MemberType, NodeWithParameters<?>> returnTypeToMustPreserveNode = new LinkedHashMap<>();

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

    List<? extends NodeWithParameters<?>> parentNodeWithParams =
        JavaParserUtil.tryResolveNodeWithUnresolvableArguments(withArgs, fqnsToCompilationUnits);

    for (NodeWithParameters<?> callable : parentNodeWithParams) {
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
   * @param slice The slice, for reference.
   * @return An object of type {@link UnsolvedGenerationResult}, usually empty, but the close()
   *     method(s) if first time confirmation of an AutoCloseable, or if the return type is updated
   *     in a method call expression.
   */
  public UnsolvedGenerationResult addInformation(Node node, Set<Node> slice) {
    List<UnsolvedSymbolAlternates<?>> toAdd = new ArrayList<>();
    List<UnsolvedSymbolAlternates<?>> toRemove = new ArrayList<>();

    if (node instanceof ClassOrInterfaceDeclaration decl) {
      for (ClassOrInterfaceType implemented : decl.getImplementedTypes()) {
        UnsolvedClassOrInterfaceAlternates syntheticType =
            (UnsolvedClassOrInterfaceAlternates)
                findExistingAndUpdateFQNs(fullyQualifiedNameGenerator.getFQNsFromType(implemented));

        if (syntheticType != null) {
          syntheticType.setType(UnsolvedClassOrInterfaceType.INTERFACE);
          syntheticType.removeAndBlockSealedness(Sealedness.FINAL);
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
          syntheticType.removeAndBlockSealedness(Sealedness.FINAL);
        }
      }
      for (ClassOrInterfaceType permitted : decl.getPermittedTypes()) {
        UnsolvedClassOrInterfaceAlternates syntheticType =
            (UnsolvedClassOrInterfaceAlternates)
                findExistingAndUpdateFQNs(fullyQualifiedNameGenerator.getFQNsFromType(permitted));

        if (syntheticType != null) {
          if (!decl.isInterface()) {
            syntheticType.setType(UnsolvedClassOrInterfaceType.CLASS);
            syntheticType.ensureSuperClass(
                new SolvedMemberType(decl.getFullyQualifiedName().get()));
          } else {
            syntheticType.forceSuperInterface(
                new SolvedMemberType(decl.getFullyQualifiedName().get()));
          }

          // Sealedness best effort should be final unless we have evidence against it
          syntheticType.addSealedness(Sealedness.FINAL);
          syntheticType.addSealedness(Sealedness.NON_SEALED);

          List<String> parameterTypes = null;
          for (ConstructorDeclaration constructor : decl.getConstructors()) {
            if (!slice.contains(constructor)) {
              continue;
            }

            if (parameterTypes != null
                && parameterTypes.size() <= constructor.getParameters().size()) {
              continue;
            }

            parameterTypes =
                constructor.getParameters().stream().map(p -> p.getType().toString()).toList();
          }

          if (parameterTypes != null && !parameterTypes.isEmpty()) {
            String superCall = JavaParserUtil.getDefaultConstructorCall(parameterTypes, false);

            boolean foundConstructor = false;

            for (UnsolvedSymbolAlternates<?> alternate : generatedSymbols.values()) {
              if (!(alternate instanceof UnsolvedMethodAlternates method)
                  || !alternate.getAlternateDeclaringTypes().contains(syntheticType)) {
                continue;
              }

              if (!Objects.equals(method.getName(), syntheticType.getClassName())) {
                continue;
              }

              foundConstructor = true;
              method.setContent(superCall);
            }

            if (!foundConstructor) {
              UnsolvedMethodAlternates constructor =
                  UnsolvedMethodAlternates.create(
                      syntheticType.getClassName(),
                      Set.of(new SolvedMemberType("")),
                      List.of(syntheticType),
                      List.of());

              constructor.setContent(superCall);
              toAdd.add(constructor);
            }
          }
        }
      }
    } else if (node instanceof EnumDeclaration decl) {
      for (ClassOrInterfaceType implemented : decl.getImplementedTypes()) {
        UnsolvedClassOrInterfaceAlternates syntheticType =
            (UnsolvedClassOrInterfaceAlternates)
                findExistingAndUpdateFQNs(fullyQualifiedNameGenerator.getFQNsFromType(implemented));

        if (syntheticType != null) {
          syntheticType.setType(UnsolvedClassOrInterfaceType.INTERFACE);
          syntheticType.removeAndBlockSealedness(Sealedness.FINAL);
        }
      }
    } else if (node instanceof CallableDeclaration<?> callableDecl) {
      // Both MethodDeclaration and ConstructorDeclaration can have throws clauses.
      for (ReferenceType thrownException : callableDecl.getThrownExceptions()) {
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
              fullyQualifiedNameGenerator.getFQNsForExpressionType(resource)) {
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
                    findExistingAndUpdateFQNs(fullyQualifiedNameGenerator.getFQNsFromType(refType));
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

      ensureCaughtExceptionsAreThrown(tryStmt, slice);
    } else if (node instanceof InstanceOfExpr instanceOf) {
      // If we have x : X and x instanceof Y, then X must be a supertype
      // of Y if X != Y. The JLS says (15.20.2): "If a cast of the RelationalExpression to the
      // ReferenceType would be rejected as a compile-time error, then the instanceof relational
      // expression likewise produces a compile-time error. In such a situation, the result of the
      // instanceof expression could never be true."
      //
      // This logic uses this fact to add extends clauses to synthetic classes.
      Type type;
      if (instanceOf.getPattern().isPresent()) {
        PatternExpr patternExpr = instanceOf.getPattern().get();
        type = patternExpr.getType();
      } else {
        type = instanceOf.getType();
      }

      if (Resolver.resolve(type) != null) {
        return UnsolvedGenerationResult.EMPTY;
      }

      toRemove.addAll(
          makeSyntheticTypeASubtypeOfExpressionType(
              type, instanceOf.getExpression(), "instanceof"));
    } else if (node instanceof CastExpr castExpr
        && !castExpr.getExpression().isLambdaExpr()
        && !castExpr.getExpression().isMethodReferenceExpr()) {
      // A cast (T) e compiles only if casting conversion accepts e's type to T (JLS 15.16, 5.5),
      // which is exactly the condition JLS 15.20.2 (quoted above) carries over to instanceof. A
      // synthetic T is therefore handled the same way here as it is there.
      //
      // Constraining e's type to make the cast legal would be wrong, because it would also degrade
      // the type inferred for e at its other use sites. Since Specimin chooses what T is, it can
      // instead make T a subtype of e's type, which turns the cast into a legal downcast and
      // leaves e's type alone. FullyQualifiedNameGenerator therefore reports a synthetic cast
      // target as constraining nothing, and this is where that promise is kept.
      //
      // Lambdas and method references are excluded for the same reason as there: they are poly
      // expressions with no type of their own, and the functional-interface logic handles them.
      Type type = castExpr.getType();

      if (Resolver.resolve(type) != null) {
        // Not synthetic, so Specimin does not get to choose its supertypes.
        return UnsolvedGenerationResult.EMPTY;
      }

      toRemove.addAll(
          makeSyntheticTypeASubtypeOfExpressionType(type, castExpr.getExpression(), "cast"));
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

    // Get super classes: type of LHS is a super type of the type of the RHS.
    //
    // A String += is excluded, because it is not really an assignment of the RHS to the LHS: JLS
    // 15.26.2 defines it as s = (String) (s + x), so the RHS is an operand of a string
    // concatenation and is unconstrained, exactly as in the BinaryExpr case below. Making it a
    // subtype of the LHS would demand that it be a subtype of the final class String, which no
    // type can be.
    if ((node instanceof AssignExpr assign && !isDefinitelyStringConcatenation(assign))
        || (node instanceof VariableDeclarator varDecl && varDecl.getInitializer().isPresent())
        || (node instanceof ReturnStmt returnStmt && returnStmt.getExpression().isPresent())
        || node instanceof LambdaExpr) {
      Set<MemberType> lhsType;

      Supplier<@Nullable ResolvedType> getResolvedTypeOfLHS;

      // A lambda has one result expression per return statement of its own, and each is separately
      // constrained by the target type; every other kind of node here has exactly one.
      List<Expression> rhsExpressions;

      if (node instanceof AssignExpr assignExpr) {
        Expression lhs = assignExpr.getTarget();
        rhsExpressions = List.of(assignExpr.getValue());
        lhsType =
            getMemberTypesAndExpectNonNullFromFQNSets(
                fullyQualifiedNameGenerator.getFQNsForExpressionType(lhs));

        getResolvedTypeOfLHS = () -> Resolver.calculateResolvedType(lhs);
      } else if (node instanceof VariableDeclarator varDecl) {
        Type lhs = varDecl.getType();

        if (lhs.isVarType()) {
          return UnsolvedGenerationResult.EMPTY;
        }

        rhsExpressions = List.of(varDecl.getInitializer().get());
        MemberType lhsMemberType =
            getMemberTypeFromFQNs(fullyQualifiedNameGenerator.getFQNsFromType(lhs), false);
        lhsType = lhsMemberType == null ? Set.of() : Set.of(lhsMemberType);

        getResolvedTypeOfLHS = () -> Resolver.resolve(lhs);
      } else if (node instanceof ReturnStmt returnStmt) {
        Node methodOrLambda = JavaParserUtil.findClosestMethodOrLambdaAncestor(returnStmt);

        if (methodOrLambda instanceof MethodDeclaration methodDecl) {
          Type lhs = methodDecl.getType();
          rhsExpressions = List.of(returnStmt.getExpression().get());
          MemberType lhsMemberType =
              getMemberTypeFromFQNs(fullyQualifiedNameGenerator.getFQNsFromType(lhs), false);
          lhsType = lhsMemberType == null ? Set.of() : Set.of(lhsMemberType);
          getResolvedTypeOfLHS = () -> Resolver.resolve(lhs);
        } else {
          // Do not handle here: handle when we encounter the ancestor LambdaExpr node
          return UnsolvedGenerationResult.EMPTY;
        }
      } else if (node instanceof LambdaExpr lambdaExpr) {

        // Must not call Resolver#calculateResolvedType on a lambda when its body is
        // unsolved, because JavaParser's TypeExtractor creates a circular dependency: it resolves
        // the body in order to infer the functional
        // interface's type arguments, and that is exactly the case this constraint exists to fix.
        Set<FullyQualifiedNameSet> resultTypeFQNs =
            fullyQualifiedNameGenerator.getFQNsForLambdaResultType(lambdaExpr);

        if (resultTypeFQNs == null) {
          return UnsolvedGenerationResult.EMPTY;
        }

        if (lambdaExpr.getExpressionBody().isPresent()) {
          rhsExpressions = List.of(lambdaExpr.getExpressionBody().get());
        } else {
          // Every one of the lambda's own returns is a separate result expression that the target
          // type constrains, so all of them are collected: constraining only the first would leave
          // the rest free to keep a placeholder type the target type cannot accept.
          rhsExpressions = new ArrayList<>();

          for (ReturnStmt returnStmt : JavaParserUtil.findOwnReturnStmts(lambdaExpr)) {
            if (returnStmt.getExpression().isPresent()) {
              rhsExpressions.add(returnStmt.getExpression().get());
            }
          }

          if (rhsExpressions.isEmpty()) {
            return UnsolvedGenerationResult.EMPTY;
          }
        }

        Set<MemberType> resultTypes = new LinkedHashSet<>();
        for (FullyQualifiedNameSet resultTypeFQN : resultTypeFQNs) {
          MemberType resultType = getMemberTypeFromFQNs(resultTypeFQN, false);

          if (resultType != null) {
            resultTypes.add(resultType);
          }
        }

        lhsType = resultTypes;
        // The result type is known only by name here, so there is no ResolvedType to hand over.
        // This is a supported input: it selects addSuperType over forceSuperClass/
        // forceSuperInterface, and isNonExtendableType below falls back to deciding by name.
        getResolvedTypeOfLHS = () -> null;
      } else {
        throw new RuntimeException(
            "Expected an assignment, variable declarator, return expression, or lambda; got "
                + node.getClass()
                + " instead!");
      }

      if (lhsType.isEmpty()) {
        throw new RuntimeException("Type has not been generated for the LHS of " + node);
      }

      for (Expression rhs : rhsExpressions) {
        Set<MemberType> rhsType =
            getMemberTypesAndExpectNonNullFromFQNSets(
                fullyQualifiedNameGenerator.getFQNsForExpressionType(rhs));

        // There is a chance the LHS type cannot be extended. In that case, we cannot make the RHS
        // a subtype. This can happen when the RHS is an unsolved method and we created a synthetic
        // type for its return type, but we need to modify that return type to use a generic
        // unconstrained type variable instead.
        UnsolvedMethodAlternates methodWithPotentiallyUnconstrainedReturnType =
            rhs.isMethodCallExpr()
                ? findGeneratedMethodFromMethodCall(rhs.asMethodCallExpr())
                : null;

        if (rhsType.isEmpty()) {
          // A generated method that returns one of its own type variables reports no type at this
          // call site at all (see
          // FullyQualifiedNameGenerator#getExpressionTypesIfRepresentsGenerated)
          // because the call site does not constrain it. There is then no placeholder return type
          // for the code below to reconcile with the LHS, so there is nothing to do here.
          if (methodWithPotentiallyUnconstrainedReturnType != null
              && methodWithPotentiallyUnconstrainedReturnType.getReturnTypes().stream()
                  .allMatch(methodWithPotentiallyUnconstrainedReturnType::isOwnTypeVariable)) {
            continue;
          }

          throw new RuntimeException("Type has not been generated for the RHS of " + node);
        }

        // Could this site's target type reject what the method currently returns? See
        // isNonExtendableType, which uses this to decide when to fall back to an unconstrained
        // type variable to satisfy all use sites.
        boolean returnTypeCanConflictWithLHS = !lhsType.containsAll(rhsType);

        boolean handledAsNonExtendable = false;
        if (methodWithPotentiallyUnconstrainedReturnType != null
            && isNonExtendableType(
                getResolvedTypeOfLHS.get(), lhsType, returnTypeCanConflictWithLHS)) {
          toRemove.addAll(useUnconstrainedReturnType(methodWithPotentiallyUnconstrainedReturnType));

          handledAsNonExtendable = true;
        }

        if (!handledAsNonExtendable) {
          // This test checks for conflicts that making the LHS a supertype of the RHS cannot
          // repair, because every candidate return type already exists and so there is no generated
          // type to give a supertype to. An unconstrained return type is used instead.
          if (methodWithPotentiallyUnconstrainedReturnType != null
              && returnTypeCanConflictWithLHS
              && rhsType.stream().allMatch(type -> type instanceof SolvedMemberType)) {
            toRemove.addAll(
                useUnconstrainedReturnType(methodWithPotentiallyUnconstrainedReturnType));
          } else {
            makeLHSSupertypeOfRHS(lhsType, rhsType, getResolvedTypeOfLHS);
          }
        }
      }
    } else if (node instanceof MethodCallExpr
        || node instanceof ObjectCreationExpr
        || node instanceof ExplicitConstructorInvocationStmt
        || (node instanceof EnumConstantDeclaration enumConstantDeclaration
            && enumConstantDeclaration.getArguments().isNonEmpty())) {
      NodeWithArguments<?> nodeWithArgs = (NodeWithArguments<?>) node;

      ResolvedMethodLikeDeclaration resolved = null;
      if (!(node instanceof EnumConstantDeclaration)) {
        resolved = (ResolvedMethodLikeDeclaration) Resolver.resolve((Resolvable<?>) nodeWithArgs);
      }

      if (resolved == null && node instanceof MethodCallExpr methodCall) {
        Object decl =
            JavaParserUtil.tryFindCorrespondingDeclarationForConstraintQualifiedExpression(
                methodCall);

        if (decl instanceof ResolvedMethodDeclaration methodDecl) {
          resolved = methodDecl;
        }
      }

      if (resolved != null) {

        Node asAst = JavaParserUtil.tryFindAttachedNode(resolved, fqnsToCompilationUnits);

        // If this call invokes a callable whose throws clause names a synthetic exception, and
        // that exception is not caught or declared in the enclosing context, then the exception
        // must be unchecked for the slice to compile.
        if (asAst instanceof CallableDeclaration<?> callee) {
          toRemove.addAll(handleUnhandledCheckedExceptions((Node) nodeWithArgs, callee));
        }

        for (int i = 0; i < nodeWithArgs.getArguments().size(); i++) {
          MemberType lhsType;
          Set<MemberType> rhsType =
              getMemberTypesAndExpectNonNullFromFQNSets(
                  fullyQualifiedNameGenerator.getFQNsForExpressionType(
                      nodeWithArgs.getArgument(i)));

          Supplier<@Nullable ResolvedType> getResolvedTypeOfLHS;

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
            getResolvedTypeOfLHS =
                () -> {
                  try {
                    return param.getType();
                  } catch (UnsolvedSymbolException ex) {
                    return null;
                  }
                };
          } catch (UnsolvedSymbolException ex) {
            // asAst is a callable here: if the parameter type is unresolvable, then it must be
            // in the project, because JDK parameters will always be resolvable. An enum's
            // implicit methods also cannot reach this branch, since their parameter types always
            // resolve.
            if (!(asAst instanceof NodeWithParameters<?> calleeWithParams)) {
              throw new RuntimeException("asAst must be a callable, but was: " + asAst, ex);
            }

            Type type = calleeWithParams.getParameter(i).getType();

            lhsType =
                getMemberTypeFromFQNs(fullyQualifiedNameGenerator.getFQNsFromType(type), false);
            getResolvedTypeOfLHS = () -> Resolver.resolve(type);
          }

          if (rhsType.isEmpty()) {
            throw new RuntimeException(
                "Type has not been generated for " + nodeWithArgs.getArgument(i));
          }

          if (lhsType == null) {
            throw new RuntimeException(
                "Type has not been generated for the LHS of parameter " + i + " of " + node);
          }

          makeLHSSupertypeOfRHS(Set.of(lhsType), rhsType, getResolvedTypeOfLHS);
        }
      } else {
        List<? extends NodeWithParameters<?>> withUnresolvableArgs =
            JavaParserUtil.tryResolveNodeWithUnresolvableArguments(
                nodeWithArgs, fqnsToCompilationUnits);

        if (withUnresolvableArgs.isEmpty()) {
          UnsolvedMethodAlternates genMethod;
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
                fullyQualifiedNameGenerator.generateAllMethodFQNsWithTypeVariableCorrection(
                    methodCall,
                    methodScope.stream()
                        .map(
                            s -> {
                              UnsolvedClassOrInterfaceAlternates type =
                                  (UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(s);

                              if (type == null) {
                                throw new RuntimeException(
                                    "Type should be generated already: " + s);
                              }

                              return type;
                            })
                        .toList(),
                    false);
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
            // We reach this branch when no matching constructor declaration could be found for a
            // constructor call whose arguments are not all resolvable. If the constructor's
            // declaring type is nevertheless a fully-known class (e.g. a JDK type such as
            // IllegalStateException, or a source type without a matching-arity constructor), then
            // there is no synthetic constructor to generate: the declaring type is not one of the
            // synthetic symbols, so getFQNsForUnsolvableConstructor would find a null scope and
            // crash. Since the declaring type's constructor parameters are already known, there are
            // no synthetic parameter types to constrain from this call, so there is nothing to do.
            if (isKnownConstructorDeclaringType(node)) {
              return UnsolvedGenerationResult.EMPTY;
            }
            genMethod =
                (UnsolvedMethodAlternates)
                    findExistingAndUpdateFQNs(getFQNsForUnsolvableConstructor(node));

            // The synthetic constructor's parameter types are computed from the current (best
            // known) types of the arguments. If those argument types have since been refined (for
            // example, a synthetic placeholder type was later unified with a more specific type),
            // the recomputed constructor signature can no longer match the one originally
            // generated, so no alternates are found. This is only a best-effort step to constrain
            // argument types against the constructor's parameters, so if the constructor cannot be
            // located there is nothing further to constrain here.
            if (genMethod == null) {
              return UnsolvedGenerationResult.EMPTY;
            }
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
            Supplier<@Nullable ResolvedType> getResolvedTypeOfLHS = () -> null;

            if (rhsType.isEmpty()) {
              throw new RuntimeException(
                  "Type has not been generated for " + nodeWithArgs.getArgument(i));
            }

            if (lhsType.isEmpty()) {
              throw new RuntimeException(
                  "Type has not been generated for the LHS of parameter " + i + " of " + node);
            }

            makeLHSSupertypeOfRHS(lhsType, rhsType, getResolvedTypeOfLHS);
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
            Supplier<@Nullable ResolvedType> getResolvedTypeOfLHS;

            if (withUnresolvableArgs.size() == 1) {
              getResolvedTypeOfLHS =
                  () -> Resolver.resolve(withUnresolvableArgs.get(0).getParameter(iCopy).getType());
            } else {
              getResolvedTypeOfLHS = () -> null;
            }

            if (rhsType.isEmpty()) {
              throw new RuntimeException(
                  "Type has not been generated for " + nodeWithArgs.getArgument(i));
            }

            if (lhsType.isEmpty()) {
              throw new RuntimeException(
                  "Type has not been generated for the LHS of parameter " + i + " of " + node);
            }

            makeLHSSupertypeOfRHS(lhsType, rhsType, getResolvedTypeOfLHS);
          }
        }
      }
    }
    // If the node is a binary expression, sometimes we can get more type constraints
    // i.e., x < y means x and y must be numbers
    else if (node instanceof BinaryExpr binaryExpr) {
      BinaryExpr.Operator operator = binaryExpr.getOperator();
      Expression left = binaryExpr.getLeft();
      Expression right = binaryExpr.getRight();

      Set<MemberType> typesToReplace = new LinkedHashSet<>();

      // == and != are inconclusive for types, and so is a + that is a string concatenation: JLS
      // 15.18.1 allows the other operand of a concatenation to have any type at all, so unlike
      // every other operator here it tells us nothing about either side. Constraining an operand
      // to getTypesForOp("+") anyway would overwrite whatever type its real use sites established
      // with an arbitrary member of that set, which is how the operand of a "" + x used to come
      // out as an int.
      if (operator != BinaryExpr.Operator.EQUALS
          && operator != BinaryExpr.Operator.NOT_EQUALS
          && !isDefinitelyStringConcatenation(binaryExpr)) {
        for (String validType : JavaLangUtils.getTypesForOp(operator.asString())) {
          if (JavaParserUtil.isAClassName(validType)) {
            validType = "java.lang." + validType;
          }
          typesToReplace.add(new SolvedMemberType(validType));
        }
      }

      if (!typesToReplace.isEmpty()) {
        for (Expression side : Set.of(left, right)) {
          if (side.isMethodCallExpr()) {
            UnsolvedMethodAlternates methodAlternates =
                findGeneratedMethodFromMethodCall(side.asMethodCallExpr());

            if (methodAlternates != null
                && methodAlternates.getReturnTypes().stream().noneMatch(typesToReplace::contains)) {
              // Set all to same return type which removes duplicates; then we can add
              // our whole set to make sure that all the old types are gone
              methodAlternates.setReturnType(typesToReplace.iterator().next());
              methodAlternates.addReturnTypes(typesToReplace);
            }
          } else if (side.isFieldAccessExpr() || side.isNameExpr()) {
            UnsolvedFieldAlternates fieldAlternates = findGeneratedFieldFromUsage(side);

            if (fieldAlternates != null
                && fieldAlternates.getTypes().stream().noneMatch(typesToReplace::contains)) {
              fieldAlternates.replaceAllOldFieldTypes(typesToReplace);
            }
          }
        }
      }
    }

    return new UnsolvedGenerationResult(toAdd, toRemove);
  }

  /**
   * Returns true if the given binary expression is a string concatenation rather than an arithmetic
   * addition. Per JLS 15.18.1 a {@code +} is a concatenation as soon as either operand is a {@code
   * String}, and then the other operand may have any type at all.
   *
   * <p>An operand whose type Specimin is unsure of is not treated as a String, so an expression
   * that is really a concatenation between two unsolved operands is still reported as an addition.
   * That is, this method is conservative: it only returns true if it can prove that one side of the
   * operation is really java.lang.String.
   *
   * @param binaryExpr a binary expression
   * @return true if the expression is definitely a string concatenation
   */
  private boolean isDefinitelyStringConcatenation(BinaryExpr binaryExpr) {
    if (binaryExpr.getOperator() != BinaryExpr.Operator.PLUS) {
      return false;
    }

    for (Expression operand : List.of(binaryExpr.getLeft(), binaryExpr.getRight())) {
      if (isDefinitelyString(operand)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the given compound assignment is a string concatenation rather than an
   * arithmetic addition. Per JLS 15.26.2 a {@code s += x} is exactly {@code s = (String) (s + x)}
   * when {@code s} is a {@code String}, so {@code x} is a concatenation operand and may have any
   * type at all.
   *
   * <p>Only the target can make the operator a concatenation: {@code s += x} where {@code s} is not
   * a String and {@code x} is one does not compile, so a String on the value side is no evidence.
   *
   * @param assignExpr an assignment expression
   * @return true if the expression is definitely a string concatenation
   */
  private boolean isDefinitelyStringConcatenation(AssignExpr assignExpr) {
    return assignExpr.getOperator() == AssignExpr.Operator.PLUS
        && isDefinitelyString(assignExpr.getTarget());
  }

  /**
   * Returns true if Specimin can prove that the given expression's type is {@code
   * java.lang.String}. An expression whose type Specimin is unsure of is not a String by this
   * definition: the callers use it to decide that a {@code +} is a concatenation, and guessing
   * wrong there would suppress a real constraint.
   *
   * @param expr an expression
   * @return true if the expression's type is definitely java.lang.String
   */
  private boolean isDefinitelyString(Expression expr) {
    return JavaLangUtils.isJavaLangString(
        FullyQualifiedNameSet.getSoleErasedFqn(
            fullyQualifiedNameGenerator.getFQNsForExpressionType(expr)));
  }

  /**
   * Replaces a generated method's return type with an unconstrained type variable, for use when
   * some requirement on the result of a call to that method cannot be met by any type Specimin
   * could generate. A type variable can be instantiated separately at every call site, so it meets
   * every such requirement at once, at the cost of saying nothing about the result.
   *
   * <p>A placeholder return type that this leaves with no remaining use is deleted rather than
   * emitted as an unreferenced file.
   *
   * @param method the generated method whose return type cannot serve some use of its result
   * @return the placeholder types that are no longer needed, for the caller to drop from the slice
   */
  private List<UnsolvedSymbolAlternates<?>> useUnconstrainedReturnType(
      UnsolvedMethodAlternates method) {
    Set<UnsolvedClassOrInterfaceAlternates> symbolsToRemove = new HashSet<>();
    for (MemberType returnType : method.getReturnTypes()) {
      if (returnType instanceof UnsolvedMemberType unsolvedReturnType
          && unsolvedReturnType.usesGeneratedName()) {
        // Check to see if it is unused everywhere (no methods or fields)
        if (findAllMembers(unsolvedReturnType.getUnsolvedType()).isEmpty()) {
          symbolsToRemove.add(unsolvedReturnType.getUnsolvedType());
        }
      }
    }

    method.setUnconstrainedReturnType();
    for (UnsolvedClassOrInterfaceAlternates symbolToRemove : symbolsToRemove) {
      removeTypeAndReplaceUses(
          new UnsolvedMemberType(symbolToRemove, 0, List.of(), false),
          new SolvedMemberType("java.lang.Object"));
    }

    return new ArrayList<>(symbolsToRemove);
  }

  /**
   * Records that a synthetic type must be a subtype of the type of the given expression.
   *
   * <p>Shared by the {@code instanceof} and cast cases of {@link #addInformation}, which impose the
   * same requirement for the same reason: the cast must be accepted by casting conversion (JLS
   * 5.5), and JLS 15.20.2 gives {@code x instanceof T} that same legality condition. When {@code T}
   * is synthetic, Specimin chooses its supertypes, so it can satisfy the condition without
   * constraining {@code x}'s type.
   *
   * @param syntheticType the type named by the instanceof or the cast. Must not be resolvable; the
   *     caller is responsible for checking that, since a type that already exists has supertypes
   *     Specimin does not get to choose.
   * @param operand the operand of the instanceof or the cast
   * @param kind how to describe the construct in error messages, e.g. "cast"
   * @return the placeholder types that are no longer needed, for the caller to drop from the slice
   */
  private List<UnsolvedSymbolAlternates<?>> makeSyntheticTypeASubtypeOfExpressionType(
      Type syntheticType, Expression operand, String kind) {
    Set<MemberType> operandTypes =
        getMemberTypesAndExpectNonNullFromFQNSets(
            fullyQualifiedNameGenerator.getFQNsForExpressionType(operand));

    if (operandTypes.isEmpty()) {
      // No information about the operand's type. This happens when the operand's type is
      // unconstrained -- for example, a call to a synthetic method whose return type is one of its
      // own type variables, which can be instantiated to anything the context needs. There is
      // nothing to make the synthetic type a subtype of, and nothing needs to be: the type
      // variable is instantiated to the synthetic type at this site, which makes the cast or
      // instanceof legal on its own.
      return List.of();
    }

    // Nothing can be made a subtype of a final class, or of any other non-extendable type. Note
    // that these are the types Specimin inferred for the operand, which may be an
    // over-approximation of its real type, so this is worth checking even though a cast or
    // instanceof naming a synthetic supertype of such a type could not appear in a program that
    // compiles. Adding the supertype would emit a class that extends a final class or a
    // primitive, which cannot compile in any context.
    Set<MemberType> extendableOperandTypes =
        operandTypes.stream()
            .filter(
                operandType ->
                    !(operandType instanceof SolvedMemberType)
                        || operandType.getFullyQualifiedNames().stream()
                            .noneMatch(
                                fqn ->
                                    JavaParserUtil.isNonExtendableTypeName(
                                        fqn, fqnsToCompilationUnits)))
            .collect(Collectors.toCollection(LinkedHashSet::new));

    if (extendableOperandTypes.isEmpty()) {
      // The requirement cannot be met by making the synthetic type a subtype, so it has to be met
      // on the other side: if the operand is a call to a generated method, that method's return
      // type is the thing standing in the way, and an unconstrained one accommodates this site
      // along with every other. Repairing it here rather than leaving the requirement dropped is
      // what makes the outcome independent of the order these sites are visited in -- the site
      // that assigns the result elsewhere may already have been visited, and would then see a
      // return type that satisfies it and no reason to act.
      if (operand.isMethodCallExpr()) {
        UnsolvedMethodAlternates generatedMethod =
            findGeneratedMethodFromMethodCall(operand.asMethodCallExpr());

        if (generatedMethod != null) {
          return useUnconstrainedReturnType(generatedMethod);
        }
      }

      return List.of();
    }

    UnsolvedClassOrInterfaceAlternates unsolvedSyntheticType =
        (UnsolvedClassOrInterfaceAlternates)
            findExistingAndUpdateFQNs(fullyQualifiedNameGenerator.getFQNsFromType(syntheticType));

    if (unsolvedSyntheticType == null) {
      throw new RuntimeException(
          "Unsolved "
              + kind
              + " type when all unsolved symbols should be generated: "
              + syntheticType);
    }
    unsolvedSyntheticType.addSuperType(extendableOperandTypes);

    return List.of();
  }

  /**
   * Removes the synthetic type from the generated symbols map, and replaces all uses of this
   * synthetic type in other generated symbols with the provided replaceWith type.
   *
   * @param syntheticType the synthetic type to remove and replace uses of
   * @param replaceWith the type to replace uses of the synthetic type with
   */
  private void removeTypeAndReplaceUses(
      UnsolvedMemberType syntheticType, MemberType... replaceWith) {
    if (!generatedSymbols.containsKey(syntheticType.getFullyQualifiedNames().iterator().next())) {
      return;
    }

    Set<UnsolvedSymbolAlternates<?>> symbolsToRemove = new HashSet<>();
    symbolsToRemove.add(syntheticType.getUnsolvedType());

    Map<Set<String>, UnsolvedSymbolAlternates<?>> oldFQNsToUpdated = new HashMap<>();

    List<UnsolvedMemberType> unsolvedReplacements = new ArrayList<>();
    for (MemberType type : replaceWith) {
      if (type instanceof UnsolvedMemberType unsolved) {
        unsolvedReplacements.add(unsolved);
      }
    }

    for (UnsolvedSymbolAlternates<?> symbol : generatedSymbols.values()) {
      if (symbol instanceof UnsolvedMethodAlternates method) {
        if (method.getAlternates().stream()
            .anyMatch(alt -> alt.getParameterList().contains(syntheticType))) {
          oldFQNsToUpdated.put(method.getFullyQualifiedNames(), method);
          method.replaceParameterType(syntheticType, Set.of(replaceWith));
        }
        if (method.getReturnTypes().contains(syntheticType)) {
          method.replaceReturnType(syntheticType, Set.of(replaceWith));
        }
      } else if (symbol instanceof UnsolvedFieldAlternates field) {
        if (field.getTypes().contains(syntheticType)) {
          field.replaceAllOldFieldTypes(Set.of(replaceWith));
        }
      }

      if (!symbol.getAlternateDeclaringTypes().contains(syntheticType.getUnsolvedType())) {
        continue;
      }

      // All are solved; this symbol exists in the codebase/JDK
      if (unsolvedReplacements.isEmpty()) {
        if (symbol.getAlternateDeclaringTypes().size() == 1) {
          symbolsToRemove.add(symbol);
        }
      } else {
        int index = symbol.getAlternateDeclaringTypes().indexOf(syntheticType.getUnsolvedType());
        symbol.getAlternateDeclaringTypes().remove(index);
        for (int i = unsolvedReplacements.size() - 1; i >= 0; i--) {
          symbol
              .getAlternateDeclaringTypes()
              .add(index, unsolvedReplacements.get(i).getUnsolvedType());
        }
      }
    }

    for (UnsolvedSymbolAlternates<?> symbol : symbolsToRemove) {
      removeSymbolFromGeneratedSymbolsMap(symbol);
      // Call this after because removeSymbolFromGeneratedSymbolsMap relies on FQNs, but if we
      // remove
      // the synthetic type before, then that FQN will be gone and we won't be able to find the
      // symbol in the map
      symbol.getAlternateDeclaringTypes().remove(syntheticType.getUnsolvedType());
    }

    for (Map.Entry<Set<String>, UnsolvedSymbolAlternates<?>> entry : oldFQNsToUpdated.entrySet()) {
      for (String fqn : entry.getKey()) {
        generatedSymbols.remove(fqn);
      }
      for (String fqn : entry.getValue().getFullyQualifiedNames()) {
        generatedSymbols.put(fqn, entry.getValue());
      }
    }
  }

  /**
   * Finds all generated symbols that are members (fields/methods) of the given type.
   *
   * @param type The encapsulating type
   * @return A set of generated fields/methods that may be in the given type
   */
  private Set<UnsolvedSymbolAlternates<?>> findAllMembers(UnsolvedClassOrInterfaceAlternates type) {
    return generatedSymbols.entrySet().stream()
        .filter(
            entry -> {
              int index = entry.getKey().indexOf("#");

              if (index == -1) {
                return false;
              }

              String declaringTypeFQN = entry.getKey().substring(0, index);
              return type.getFullyQualifiedNames().contains(declaringTypeFQN);
            })
        .map(Map.Entry::getValue)
        .collect(Collectors.toSet());
  }

  /**
   * Returns true if the type of a value assigned to a variable tells us anything about that
   * variable's type. The null type does not: {@code null} is assignable to every reference type
   * (JLS 4.1), so a {@code null} initializer or assignment is compatible with any declared type.
   *
   * @param type the resolved type of an initializer or of an assigned value, or null if it could
   *     not be resolved
   * @return true if type constrains the assigned variable's type
   */
  @EnsuresNonNullIf(result = true, expression = "#1")
  private static boolean constrainsVariableType(@Nullable ResolvedType type) {
    return type != null && !type.isNull();
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
    ResolvedMethodDeclaration resolvedMethod = Resolver.resolve(methodCall);
    Node ast = null;

    if (resolvedMethod == null) {
      potentialScopeFQNs = fullyQualifiedNameGenerator.getFQNsForExpressionLocation(methodCall);
    } else {
      // Potential scope is all unsolvable ancestors
      ast = JavaParserUtil.tryFindAttachedNode(resolvedMethod, fqnsToCompilationUnits);
      if (ast == null) {
        return;
      }
    }

    if (ast != null) {
      List<ClassOrInterfaceType> unsolvableAncestors =
          JavaParserUtil.getAllUnsolvableAncestors(
              JavaParserUtil.getClassLikeOrEnclosing(ast), fqnsToCompilationUnits);

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
        fullyQualifiedNameGenerator.generateAllMethodFQNsWithTypeVariableCorrection(
            methodCall,
            potentialScopeFQNs.stream()
                .map(
                    s -> {
                      UnsolvedClassOrInterfaceAlternates type =
                          (UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(s);

                      if (type == null) {
                        throw new RuntimeException("Type should be generated already: " + s);
                      }

                      return type;
                    })
                .toList(),
            false);

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
          "Unresolvable method for "
              + methodCall
              + " is not generated when all unsolved symbols should be: "
              + potentialFQNs);
    }

    if (methodCall.hasScope()) {
      Set<ResolvedType> potentialTypes = new LinkedHashSet<>();
      Expression scope = methodCall.getScope().get();
      ResolvedValueDeclaration resolved;
      if (scope.isFieldAccessExpr()) {
        resolved = Resolver.resolve(scope.asFieldAccessExpr());
      } else if (scope.isNameExpr()) {
        resolved = Resolver.resolve(scope.asNameExpr());
      } else {
        // If not a NameExpr or FieldAccessExpr, then we can't gain any more information, since
        // the type of the scope is unsolved.
        return;
      }

      if (resolved != null) {

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
          if (varDecl.getInitializer().isPresent()) {
            ResolvedType resolvedType =
                Resolver.calculateResolvedType(varDecl.getInitializer().get());

            if (!constrainsVariableType(resolvedType)) {
              continue;
            }

            potentialTypes.add(resolvedType);
          }
        }
      }

      // Now, find all places where the NameExpr/FieldAccessExpr is set to another type
      TypeDeclaration<?> typeDecl = JavaParserUtil.getEnclosingClassLike(methodCall);

      for (AssignExpr assignExpr : typeDecl.findAll(AssignExpr.class)) {
        if (assignExpr.getOperator() == AssignExpr.Operator.ASSIGN
            && assignExpr.getTarget().toString().equals(scope.toString())) {
          ResolvedType resolvedType = Resolver.calculateResolvedType(assignExpr.getValue());

          if (constrainsVariableType(resolvedType)) {
            potentialTypes.add(resolvedType);
          }
        }
      }

      String methodSignature = potentialFQNs.iterator().next();
      methodSignature = methodSignature.substring(potentialFQNs.iterator().next().indexOf('#') + 1);

      List<ResolvedType> resolvedReturnTypes = new ArrayList<>();
      List<UnsolvedMemberType> unsolvedReturnTypes = new ArrayList<>();

      for (ResolvedType type : potentialTypes) {
        // Check to see if any of these contain the same method signature; if so, we can
        // update the return type of the current generated one to match it

        // Must be a reference type: the null type was filtered out above, and for any other
        // non-reference type the method would be solvable, which we checked already
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
   * A catch clause for a checked exception type E is a compile-time error unless the corresponding
   * try block can throw a checked exception that is a subclass or superclass of E (JLS 11.2.3).
   * When the try block's only candidate throwers are synthetic methods, nothing in the output
   * throws E, so this method adds E to the throws clause of one of those synthetic methods.
   *
   * <p>This method does nothing when the try block calls no synthetic method, since there is
   * nothing that Specimin could soundly change in that case: either the original code already
   * throws the caught exception (and the slice preserves that), or the code did not compile to
   * begin with.
   *
   * @param tryStmt the try statement whose catch clauses should be made legal
   * @param slice the slice, used to find the other call sites of the candidate methods
   */
  private void ensureCaughtExceptionsAreThrown(TryStmt tryStmt, Set<Node> slice) {
    if (!tryStmt.getResources().isEmpty()) {
      // A resource's close() method is another source of exceptions. Synthetic resources get a
      // close() that throws java.lang.Exception, which legalizes any catch clause, so there is
      // never anything to do here; solved resources are not worth the trouble of enumerating.
      return;
    }

    if (tryStmt.getCatchClauses().isEmpty()) {
      return;
    }

    // The synthetic methods called in the try block, in source order. These are the only methods
    // whose throws clauses Specimin is free to change.
    List<UnsolvedMethodAlternates> candidates = new ArrayList<>();
    for (MethodCallExpr call : tryStmt.getTryBlock().findAll(MethodCallExpr.class)) {
      if (Resolver.resolve(call) != null) {
        continue;
      }

      UnsolvedMethodAlternates generated = findGeneratedMethodFromMethodCall(call);
      if (generated != null && !candidates.contains(generated)) {
        candidates.add(generated);
      }
    }

    if (candidates.isEmpty()) {
      return;
    }

    // The exception types that the try block can already throw, each expanded to include its
    // supertypes so that the subclass check below is a set intersection.
    List<Set<String>> thrownSupertypes = getExceptionsThrowableFromTryBlock(tryStmt);

    for (CatchClause clause : tryStmt.getCatchClauses()) {
      Type parameterType = clause.getParameter().getType();
      List<Type> caughtTypes =
          parameterType.isUnionType()
              ? new ArrayList<>(parameterType.asUnionType().getElements())
              : List.of(parameterType);

      for (Type caught : caughtTypes) {
        Set<String> caughtFqns = fullyQualifiedNameGenerator.getFQNsFromType(caught).erasedFqns();
        Set<String> caughtSupertypes =
            FullyQualifiedNameGenerator.getFQNsOfSelfAndSupertypes(
                Resolver.resolve(caught), caughtFqns);

        // Unchecked exceptions may always be caught.
        if (caughtSupertypes.contains("java.lang.RuntimeException")
            || caughtSupertypes.contains("java.lang.Error")) {
          continue;
        }

        // Catching Exception or Throwable is always legal, no matter what the try block throws.
        if (caughtFqns.contains("java.lang.Exception")
            || caughtFqns.contains("java.lang.Throwable")) {
          continue;
        }

        boolean alreadyThrown = false;
        for (Set<String> thrown : thrownSupertypes) {
          // Legal if the thrown type is a subclass of the caught type, or vice versa.
          if (!Collections.disjoint(thrown, caughtFqns)
              || !Collections.disjoint(caughtSupertypes, thrown)) {
            alreadyThrown = true;
            break;
          }
        }

        if (alreadyThrown) {
          continue;
        }

        MemberType caughtMemberType =
            getOrCreateMemberTypeFromFQNs(fullyQualifiedNameGenerator.getFQNsFromType(caught));

        // Any of the candidates could be the method that throws this exception, so record all of
        // those possibilities as alternates. Only the preferred candidate's throwing alternates
        // are placed first, so that the best-effort output adds the throws clause to exactly one
        // method: adding it to a method that is also called elsewhere would force those call sites
        // to handle the exception too.
        UnsolvedMethodAlternates preferred = choosePreferredThrower(candidates, tryStmt, slice);
        for (UnsolvedMethodAlternates candidate : candidates) {
          @SuppressWarnings("not.interned") // Pointer comparison is intended here.
          boolean isPreferred = candidate == preferred;
          candidate.addAlternatesWithThrownException(caughtMemberType, isPreferred);
        }

        thrownSupertypes.add(caughtSupertypes);
      }
    }
  }

  /**
   * Chooses which of the given synthetic methods should declare a caught exception. A method that
   * is called only from within the given try block is preferred, because adding a throws clause to
   * a method that is called elsewhere forces those other call sites to handle the exception as
   * well.
   *
   * @param candidates the synthetic methods called in the try block, in source order; must not be
   *     empty
   * @param tryStmt the try statement
   * @param slice the slice, used to find the other call sites of the candidates
   * @return the candidate that should declare the exception
   */
  private UnsolvedMethodAlternates choosePreferredThrower(
      List<UnsolvedMethodAlternates> candidates, TryStmt tryStmt, Set<Node> slice) {
    for (UnsolvedMethodAlternates candidate : candidates) {
      if (!isCalledOutsideOf(candidate, tryStmt.getTryBlock(), slice)) {
        return candidate;
      }
    }

    // Every candidate is called elsewhere, so there is no safe choice; the first one in source
    // order is as good as any.
    return candidates.get(0);
  }

  /**
   * Returns whether the given synthetic method is called from anywhere in the slice other than from
   * within the given block.
   *
   * @param method the synthetic method
   * @param block the block whose call sites should be ignored
   * @param slice the slice
   * @return true if the method has a call site in the slice outside of the given block
   */
  private boolean isCalledOutsideOf(UnsolvedMethodAlternates method, Node block, Set<Node> slice) {
    for (Node node : slice) {
      if (!(node instanceof MethodCallExpr call)
          || !call.getNameAsString().equals(method.getName())
          || block.isAncestorOf(call)) {
        continue;
      }

      if (Resolver.resolve(call) == null
          && method.equals(findGeneratedMethodFromMethodCall(call))) {
        return true;
      }
    }

    return false;
  }

  /**
   * Computes the exception types that the given try statement's block can throw, on a best-effort
   * basis. Each element of the result is the set of fully-qualified names of one thrown exception
   * type together with the fully-qualified names of its supertypes.
   *
   * @param tryStmt the try statement
   * @return the supertype-closed fully-qualified names of each exception the try block can throw
   */
  private List<Set<String>> getExceptionsThrowableFromTryBlock(TryStmt tryStmt) {
    List<Set<String>> result = new ArrayList<>();

    for (ThrowStmt throwStmt : tryStmt.getTryBlock().findAll(ThrowStmt.class)) {
      for (FullyQualifiedNameSet fqnSet :
          fullyQualifiedNameGenerator.getFQNsForExpressionType(throwStmt.getExpression())) {
        result.add(
            FullyQualifiedNameGenerator.getFQNsOfSelfAndSupertypes(
                Resolver.calculateResolvedType(throwStmt.getExpression()), fqnSet.erasedFqns()));
      }
    }

    for (MethodCallExpr call : tryStmt.getTryBlock().findAll(MethodCallExpr.class)) {
      ResolvedMethodDeclaration resolved = Resolver.resolve(call);

      if (resolved != null) {
        addSpecifiedExceptions(resolved, result);
        continue;
      }

      UnsolvedMethodAlternates generated = findGeneratedMethodFromMethodCall(call);
      if (generated != null) {
        for (MemberType exception : generated.getThrownExceptions()) {
          result.add(
              FullyQualifiedNameGenerator.getFQNsOfSelfAndSupertypes(
                  null, exception.getFullyQualifiedNames()));
        }
      }
    }

    for (ObjectCreationExpr creation : tryStmt.getTryBlock().findAll(ObjectCreationExpr.class)) {
      ResolvedConstructorDeclaration resolved = Resolver.resolve(creation);

      if (resolved != null) {
        addSpecifiedExceptions(resolved, result);
      }
      // Synthetic constructors are never generated with a throws clause, so there is nothing to
      // add for them.
    }

    return result;
  }

  /**
   * Adds the supertype-closed fully-qualified names of each exception in the given declaration's
   * throws clause to the given list.
   *
   * @param declaration the resolved method or constructor declaration
   * @param result the list to add to
   */
  private void addSpecifiedExceptions(
      ResolvedMethodLikeDeclaration declaration, List<Set<String>> result) {
    List<ResolvedType> specified;
    try {
      specified = declaration.getSpecifiedExceptions();
    } catch (RuntimeException e) {
      // Some resolved declarations (e.g., those backed by an incomplete type solver) throw here.
      return;
    }

    for (ResolvedType exception : specified) {
      if (!exception.isReferenceType()) {
        continue;
      }

      result.add(
          FullyQualifiedNameGenerator.getFQNsOfSelfAndSupertypes(
              exception, Set.of(exception.asReferenceType().getQualifiedName())));
    }
  }

  /**
   * Given a method call expression, try to find the unsolved symbol generated for it, if it exists.
   * Otherwise, this returns null.
   *
   * @param methodCall The method call expression
   * @return The unsolved method alternates generated for this method call, or null if it does not
   *     exist
   */
  private @Nullable UnsolvedMethodAlternates findGeneratedMethodFromMethodCall(
      MethodCallExpr methodCall) {
    List<UnsolvedClassOrInterfaceAlternates> scopes = new ArrayList<>();
    for (Set<String> fqns : fullyQualifiedNameGenerator.getFQNsForExpressionLocation(methodCall)) {
      UnsolvedClassOrInterfaceAlternates resolved =
          (UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(fqns);
      if (resolved != null) {
        scopes.add(resolved);
      }
    }

    Set<String> potentialFQNs =
        fullyQualifiedNameGenerator.generateAllMethodFQNsWithTypeVariableCorrection(
            methodCall, scopes, false);

    // Do not use findExistingAndUpdateFQNs here since we do not want to update the FQNs with side
    // effects
    for (String potentialFQN : potentialFQNs) {
      if (generatedSymbols.get(potentialFQN) instanceof UnsolvedMethodAlternates alreadyGenerated) {
        return alreadyGenerated;
      }
    }

    return null;
  }

  /**
   * Given a field access expression, try to find the unsolved symbol generated for it, if it
   * exists. Otherwise, this returns null.
   *
   * @param field The field access expression
   * @return The unsolved field alternates generated for this field access, or null if it does not
   *     exist
   */
  private @Nullable UnsolvedFieldAlternates findGeneratedFieldFromUsage(Expression field) {
    if (!(field instanceof FieldAccessExpr || field instanceof NameExpr)) {
      throw new RuntimeException("Expression must be a field access or name expression: " + field);
    }

    Collection<Set<String>> potentialScopeFQNs =
        fullyQualifiedNameGenerator.getFQNsForExpressionLocation(field);
    Set<String> potentialFQNs = new HashSet<>();

    for (Set<String> set : potentialScopeFQNs) {
      for (String potentialScopeFQN : set) {
        potentialFQNs.add(
            potentialScopeFQN + "#" + ((NodeWithSimpleName<?>) field).getNameAsString());
      }
    }

    // Do not use findExistingAndUpdateFQNs here since we do not want to update the FQNs with side
    // effects
    for (String potentialFQN : potentialFQNs) {
      if (generatedSymbols.get(potentialFQN) instanceof UnsolvedFieldAlternates alreadyGenerated) {
        return alreadyGenerated;
      }
    }

    return null;
  }

  /**
   * Can no generated class be made a subtype of the left-hand side of an assignment? If so, a
   * generated method whose result is assigned to it cannot be given a generated placeholder return
   * type, and must fall back to an unconstrained type variable instead.
   *
   * <p>Final classes are the obvious case, but they are not the only one. A primitive and an array
   * type have no declarable subtypes at all; an enum is implicitly final unless it has constant
   * bodies, none of which a generated class could be (JLS 8.9); a record is implicitly final (JLS
   * 8.10); an annotation type cannot be extended; a sealed class or interface admits only the
   * subtypes its {@code permits} clause names, which a synthetic type never is; and a type variable
   * may not be named as a superclass or superinterface (JLS 8.1.4, 8.1.5). Only a non-final,
   * non-sealed class or interface can take a generated subtype.
   *
   * <p>Every one of those kinds is reported only when {@code mayConflict} says the method's current
   * return type may be one the left-hand side rejects. A non-extendable left-hand side is not on
   * its own a reason to weaken a return type: for {@code int x = item.get();}, {@code int} is the
   * right return type for {@code get}. It is only when there is a second, incompatible use site
   * that an unconstrained type variable is required; {@code mayConflict} should be {@code true}
   * whenever there might be a second, incompatible use site.
   *
   * <p>A conflict is not always visible from the site that can act on it -- given {@code Payload p
   * = item.get(); String s = item.get();} the return type settles on {@code String}, and the {@code
   * String} assignment sees nothing wrong -- so {@code addInformation} also falls back when a site
   * sees a conflict it cannot repair by adding a supertype, and {@link
   * #makeSyntheticTypeASubtypeOfExpressionType} does the same when a cast or instanceof cannot get
   * the subtype it needs. Between them, whichever site can see the problem is the one that fixes
   * it. All of these sites are needed for soundness: removing any one of them would lead to
   * non-compilable output.
   *
   * <p>The resolved type is preferred when there is one, but the left-hand side is sometimes known
   * only by name -- a lambda's result type, for instance, is derived from the lambda's target type
   * rather than resolved. Deciding by name in that case is what lets the caller reach the same
   * unconstrained-return-type fallback it would reach for the equivalent assignment or {@code
   * return} statement, instead of going on to demand an impossible subtype.
   *
   * <p>When deciding by name, {@code lhsTypes} is a disjunction: several candidate types, and
   * within each, several candidate names, exactly one of which the left-hand side really is. Every
   * candidate must be non-extendable before this returns true, because a single extendable
   * candidate is a reading under which a subtype is still possible. Note that false is not a safe
   * default to approximate with when the answer is unclear: both answers can cost compilability,
   * since the unconstrained return type that true leads to strands any member access on the result.
   *
   * @param resolvedLHSType the resolved type of the left-hand side, or null if it is not resolvable
   * @param lhsTypes the type(s) of the left-hand side
   * @param mayConflict whether the assignment context may conflict with the existing type of the
   *     right-hand side. Only a resolved LHS type consults this; the name-keyed path ignores it.
   * @return true if no generated class could be made a subtype of the left-hand side
   */
  private boolean isNonExtendableType(
      @Nullable ResolvedType resolvedLHSType, Set<MemberType> lhsTypes, boolean mayConflict) {
    if (resolvedLHSType != null) {
      // None of these is a reference type, so none of them reaches the declaration-keyed tests
      // below. A type variable is included because a class declaration may not name one as a
      // superclass or superinterface (JLS 8.1.4, 8.1.5).
      if (resolvedLHSType.isPrimitive()
          || resolvedLHSType.isArray()
          || resolvedLHSType.isTypeVariable()) {
        return mayConflict;
      }

      if (!resolvedLHSType.isReferenceType()
          || resolvedLHSType.asReferenceType().getTypeDeclaration().isEmpty()) {
        return false;
      }

      // If LHS is solvable, there is only one
      ResolvedReferenceTypeDeclaration decl =
          resolvedLHSType.asReferenceType().getTypeDeclaration().get();

      if (decl.isEnum() || decl.isRecord() || decl.isAnnotation()) {
        return mayConflict;
      }

      // Checked before isClass() because sealed interface also cannot have synthetic subtypes.
      if (decl.toAst().orElse(null) instanceof TypeDeclaration<?> ast
          && JavaParserUtil.isSealed(ast)) {
        return mayConflict;
      }

      if (!decl.isClass()) {
        return false;
      }

      if (JavaLangUtils.isFinalJdkClass(decl.getQualifiedName())) {
        return mayConflict;
      }

      return mayConflict
          && decl.toAst().isPresent()
          && ((ClassOrInterfaceDeclaration) decl.toAst().get()).isFinal();
    }

    // "No candidates" says nothing either way, so do not let the loops below pass vacuously.
    if (lhsTypes.isEmpty()) {
      return false;
    }

    for (MemberType lhsType : lhsTypes) {
      Set<String> fqns = lhsType.getFullyQualifiedNames();

      if (fqns.isEmpty()) {
        return false;
      }

      for (String fqn : fqns) {
        if (!JavaParserUtil.isNonExtendableTypeName(fqn, fqnsToCompilationUnits)) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Makes the type of the LHS (of some pseudo-assignment) a supertype of the type of the RHS, if
   * the type of the RHS is unsolved.
   *
   * @param lhsTypes The type(s) of the LHS
   * @param rhsTypes The type(s) of the RHS
   * @param getResolvedTypeOfLHS A supplier for the resolved type of the LHS. Typically a call to
   *     resolve() or calculateResolvedType().
   */
  private void makeLHSSupertypeOfRHS(
      Set<MemberType> lhsTypes,
      Set<MemberType> rhsTypes,
      Supplier<@Nullable ResolvedType> getResolvedTypeOfLHS) {

    @Nullable ResolvedType resolved = getResolvedTypeOfLHS.get();

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

      @Nullable SolvedMemberType rhsTypeAsSolvedWithSameErasure = null;
      if (rhsTypes.size() == 1
          && rhsTypes.iterator().next() instanceof SolvedMemberType solved
          && JavaParserUtil.erase(solved.getFullyQualifiedNames().iterator().next())
              .equals(resolved.erasure().describe())) {
        rhsTypeAsSolvedWithSameErasure = solved;
      }

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

            if (rhsType.getFullyQualifiedNames().stream().noneMatch(erased::contains)) {
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

            makeLHSSupertypeOfRHS(Set.of(memberTypeBound), rhsTypeParameters, () -> bound);
          } else {
            // If the LHS were unsolved, we would make it extend every single class in the RHS; but
            // since the LHS is solved, we can't do this

            // If an issue arises in the future, we could find the unsolvable super classes of this
            // resolvable type bound and then apply these bounds there
          }
        } else if (!typeParam.isWildcard()
            && rhsTypeAsSolvedWithSameErasure != null
            && rhsTypeAsSolvedWithSameErasure.getTypeArguments().size() > i) {
          // In the case of List<String> = List<SomeConcreteSyntheticType>, we know that
          // SomeConcreteSyntheticType must be String
          // Also, since we know that the LHS is resolvable, the RHS must be a SolvedMemberType to
          // satisfy this.

          MemberType rhsTypeArg = rhsTypeAsSolvedWithSameErasure.getTypeArguments().get(i);

          if (!(rhsTypeArg instanceof UnsolvedMemberType rhsUnsolvedTypeArg)) {
            continue;
          }

          MemberType toReplaceWith =
              getMemberTypeFromFQNs(
                  fullyQualifiedNameGenerator.getFQNsForResolvedType(typeParam), false);

          if (toReplaceWith == null) {
            throw new RuntimeException(
                "Impossible error: typeParam is solved and toReplaceWith should never be null");
          }

          removeTypeAndReplaceUses(rhsUnsolvedTypeArg, toReplaceWith);
        }
      }

      return;
    }

    for (MemberType lhsType : lhsTypes) {
      for (int i = 0; i < lhsType.getTypeArguments().size(); i++) {
        MemberType typeParam = lhsType.getTypeArguments().get(i);

        if (typeParam instanceof WildcardMemberType wildcard) {
          if (wildcard.equals(WildcardMemberType.UNBOUNDED)) {
            continue;
          }

          MemberType bound = wildcard.getBound();

          if (bound == null) {
            continue;
          }
          boolean isUpperBound = wildcard.isUpperBounded();

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

            if (rhsType.getFullyQualifiedNames().stream()
                .noneMatch(lhsType.getFullyQualifiedNames()::contains)) {
              continue;
            }

            rhsTypeParameters.add(typeArg);
          }

          // ? extends with ? extends; there is no ? extends with ? super
          if (isUpperBound) {
            // Foo<? extends Bound> = Foo<A> tells us that A is a subtype of Bound.
            makeLHSSupertypeOfRHS(Set.of(bound), rhsTypeParameters, () -> null);
          } else {
            // Foo<? super Bound> = Foo<A> tells us that Bound is a subtype of A.
            makeLHSSupertypeOfRHS(rhsTypeParameters, Set.of(bound), () -> null);
          }
        } else {
          for (MemberType rhsType : rhsTypes) {
            // In the case of Foo<String> = Foo<SomeConcreteSyntheticType>, we know that
            // SomeConcreteSyntheticType must be String
            if (rhsType.getFullyQualifiedNames().stream()
                    .noneMatch(lhsType.getFullyQualifiedNames()::contains)
                || rhsType.getTypeArguments().size() <= i) {
              continue;
            }

            MemberType rhsTypeArg = rhsType.getTypeArguments().get(i);

            if (!(rhsTypeArg instanceof UnsolvedMemberType rhsUnsolvedTypeArg)
                || rhsUnsolvedTypeArg.equals(typeParam)) {
              continue;
            }

            removeTypeAndReplaceUses(rhsUnsolvedTypeArg, typeParam);
          }
        }
      }
    }
  }

  /**
   * Determines whether Specimin has already classified the declaring type of a field access as a
   * synthetic type that is not an enum.
   *
   * <p>This exists because a field access in an annotation argument is ambiguous: it may be an enum
   * constant, or a static final constant of an ordinary class. Specimin resolves that ambiguity in
   * {@link #handleFieldAccessExpr} by classifying the declaring type, and this method reports that
   * decision so that the annotation member's type can be made consistent with it. It is therefore
   * not a general test for enum-ness: it says only whether we have already decided against it. A
   * declaring type that Specimin did not synthesize is not a decision of ours, so it is not
   * reported here even if it is in fact not an enum.
   *
   * <p>The scope may have more than one candidate type. This method mirrors the rule used when the
   * field itself is generated: a single candidate that is definitively not an enum is enough to
   * decide against enum-ness, since the field's type must then be one that works for that
   * candidate. Checking only one candidate would let the two decisions disagree, which would emit a
   * constant of one type and an annotation member of another.
   *
   * <p>This must be called after the field access has been passed to {@link #inferContextImpl}, so
   * that any synthetic declaring type has already been created and classified.
   *
   * @param fieldAccess the field access to check the declaring type of
   * @return true if any candidate declaring type is a synthetic type classified as something other
   *     than an enum; false otherwise, including when every candidate was classified as an enum and
   *     when no candidate is a synthetic type at all
   */
  private boolean declaringTypeIsSyntheticNonEnum(FieldAccessExpr fieldAccess) {
    Set<FullyQualifiedNameSet> scopeFQNs =
        fullyQualifiedNameGenerator.getFQNsForExpressionType(fieldAccess.getScope());

    for (FullyQualifiedNameSet candidateFQNs : scopeFQNs) {
      UnsolvedSymbolAlternates<?> scope = findExistingAndUpdateFQNs(candidateFQNs);

      if (!(scope instanceof UnsolvedClassOrInterfaceAlternates scopeType)) {
        // The declaring type is not one Specimin synthesized, so there is no decision of ours to
        // report.
        continue;
      }

      UnsolvedClassOrInterfaceType classification = scopeType.getType();

      // UNKNOWN means either that nothing has been decided yet or that the alternates disagree.
      // Neither is evidence against enum-ness, so it must not count here: handleFieldAccessExpr
      // promotes an UNKNOWN declaring type to ENUM, and treating it as a non-enum would undo that.
      if (classification != UnsolvedClassOrInterfaceType.ENUM
          && classification != UnsolvedClassOrInterfaceType.UNKNOWN) {
        return true;
      }
    }

    return false;
  }

  /**
   * Determines whether the declaring type of an unresolvable constructor call is nevertheless a
   * fully-known class (i.e. one that the symbol solver can resolve, such as a JDK type). Such types
   * are not among the synthetic symbols, so no synthetic constructor should be generated for them.
   *
   * @param node the node representing the constructor call; either an ObjectCreationExpr or
   *     ExplicitConstructorInvocationStmt
   * @return true if the constructor's declaring type is resolvable and therefore known
   */
  private boolean isKnownConstructorDeclaringType(Node node) {
    if (node instanceof ObjectCreationExpr constructor) {
      return Resolver.resolve(constructor.getType()) != null;
    } else if (node instanceof ExplicitConstructorInvocationStmt constructor
        && !constructor.isThis()) {
      ClassOrInterfaceType superClass = JavaParserUtil.getSuperClass(node);
      return superClass != null && Resolver.resolve(superClass) != null;
    }
    return false;
  }

  /**
   * Given an unsolvable constuctor invocation (i.e., to a constructor in a synthetic class), this
   * method returns a list of fully-qualified names for the constructor invocation's argument types.
   *
   * @param node a constructor invocation: either an ExplicitConstructorInvocationStmt or an
   *     ObjectCreationExpr
   * @return a list of fully-qualified names for the constructor invocation's argument types
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
   * Once {@link #addInformation} is done, call this method to make sure all generated symbols are
   * consistent with their super type relationships.
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
   * Handles the exceptions declared in the throws clause of a callable that is invoked at the given
   * call site. If such an exception is a synthetic type that is not caught or declared in the
   * enclosing context, then the exception must be unchecked (i.e., extend {@code java.lang.Error})
   * for the slice to compile, so this method forces it to be unchecked. This is what allows
   * Specimin to notice that, e.g., a validation exception thrown by a constructor but never handled
   * by its caller must be an unchecked exception.
   *
   * @param callSite the method or constructor call expression
   * @param callee the AST of the callable being invoked
   * @return symbols that need to be removed (from making a type extend Throwable)
   */
  private List<UnsolvedSymbolAlternates<?>> handleUnhandledCheckedExceptions(
      Node callSite, CallableDeclaration<?> callee) {
    List<UnsolvedSymbolAlternates<?>> toRemove = new ArrayList<>();

    for (ReferenceType thrownException : callee.getThrownExceptions()) {
      if (!thrownException.isClassOrInterfaceType()) {
        continue;
      }

      FullyQualifiedNameSet fqns =
          fullyQualifiedNameGenerator.getFQNsFromType(thrownException.asClassOrInterfaceType());

      if (!(findExistingAndUpdateFQNs(fqns)
          instanceof UnsolvedClassOrInterfaceAlternates syntheticException)) {
        // Either the exception is not synthetic (e.g., a JDK exception whose checked-ness is
        // already known), or it has not been generated. Either way, we cannot (and need not)
        // change its checked-ness here.
        continue;
      }

      if (isCheckedExceptionHandled(callSite, fqns.erasedFqns())) {
        continue;
      }

      // The exception escapes the enclosing method or constructor unhandled, so it cannot be a
      // checked exception. Force it to be unchecked, overriding any earlier decision (e.g., from
      // the callee's throws clause) that it should be checked.
      syntheticException.ensureSuperClass(SolvedMemberType.JAVA_LANG_ERROR);
      toRemove.addAll(handleExtendThrowable(syntheticException));
    }

    return toRemove;
  }

  /**
   * Determines whether a checked exception potentially thrown at the given call site is handled
   * before it would escape the enclosing method or constructor, i.e., whether it is caught by an
   * enclosing try-catch or declared in the enclosing throws clause. If it is not handled, then a
   * synthetic exception type must be unchecked for the slice to compile.
   *
   * @param callSite the node where the exception could be thrown
   * @param exceptionFqns the possible fully-qualified names of the exception type
   * @return true if the exception is caught or declared before it escapes a method boundary
   */
  private boolean isCheckedExceptionHandled(Node callSite, Set<String> exceptionFqns) {
    Node current = callSite;
    Optional<Node> parent = current.getParentNode();

    while (parent.isPresent()) {
      Node parentNode = parent.get();

      if (parentNode instanceof TryStmt tryStmt) {
        // Only nodes within the try block are guarded by its catch clauses (nodes in a catch or
        // finally block are not).
        if (tryStmt.getTryBlock().isAncestorOf(callSite)) {
          for (CatchClause clause : tryStmt.getCatchClauses()) {
            if (catchClauseHandles(clause, exceptionFqns)) {
              return true;
            }
          }
        }
      } else if (parentNode instanceof CallableDeclaration<?> callable) {
        // We have reached the enclosing method or constructor without the exception being caught,
        // so it is handled only if it is declared in the throws clause.
        return throwsClauseHandles(callable.getThrownExceptions(), exceptionFqns);
      } else if (parentNode instanceof LambdaExpr) {
        // A checked exception cannot escape a lambda body to the enclosing method, so if we reach a
        // lambda boundary without the exception being caught, it must be unchecked.
        return false;
      }

      current = parentNode;
      parent = current.getParentNode();
    }

    return false;
  }

  /**
   * Returns whether the given catch clause catches the given exception, based on an exact type
   * match (see {@link #typeHandlesException}).
   *
   * @param clause the catch clause
   * @param exceptionFqns the possible fully-qualified names of the exception type
   * @return true if the catch clause handles the exception
   */
  private boolean catchClauseHandles(CatchClause clause, Set<String> exceptionFqns) {
    Type caught = clause.getParameter().getType();

    if (caught.isUnionType()) {
      for (ReferenceType element : caught.asUnionType().getElements()) {
        if (typeHandlesException(element, exceptionFqns)) {
          return true;
        }
      }
      return false;
    }

    return typeHandlesException(caught, exceptionFqns);
  }

  /**
   * Returns whether the given throws clause declares the given exception, based on an exact type
   * match (see {@link #typeHandlesException}).
   *
   * @param thrownExceptions the throws clause
   * @param exceptionFqns the possible fully-qualified names of the exception type
   * @return true if the throws clause declares the exception
   */
  private boolean throwsClauseHandles(
      NodeList<ReferenceType> thrownExceptions, Set<String> exceptionFqns) {
    for (ReferenceType thrownException : thrownExceptions) {
      if (typeHandlesException(thrownException, exceptionFqns)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns whether the given type (from a catch or throws clause) handles an exception with the
   * given possible fully-qualified names. A type handles the exception only if it names the exact
   * exception type.
   *
   * <p>We deliberately require an exact match rather than a subtyping check. We are working with an
   * AST and do not have access to the full type hierarchy here, so we cannot decide in general
   * whether the catch/throws type is a supertype of the exception (e.g., whether a synthetic
   * exception is a subclass of {@code java.lang.IOException} that a {@code catch (IOException)}
   * would handle). To keep the tool's behavior predictable, this approximation is deliberately
   * one-sided: it only ever concludes "handled" when it is certain, and otherwise concludes "not
   * handled", which causes the exception to be made unchecked. Making an exception unchecked is
   * always safe, because an unchecked exception never needs to be caught or declared. As a
   * consequence, a broad handler ({@code catch (Exception)} / {@code catch (Throwable)}) is treated
   * exactly like any other non-matching supertype ({@code catch (IOException)}): none of them are
   * treated as handling the exception. This also matches Specimin's existing convention that an
   * exception is treated as checked only when its exact type appears in a throws or catch clause.
   *
   * <p>The match must also cover every possibility. Both the catch/throws type and the exception
   * are represented as sets of <em>possible</em> fully-qualified names (a type reference may be
   * ambiguous, e.g., because of a wildcard import). We require the two sets to be exactly equal, so
   * that every possible FQN of the exception is also a possible FQN of the catch/throws type and
   * vice versa. A merely non-empty intersection is not enough: if the two sets only partially
   * overlap (e.g., {@code {A, B}} versus {@code {A, C}}), then the clause might refer to a
   * different type than the exception, so we cannot be certain it is handled and we conservatively
   * report "not handled" and let the exception become unchecked. (Two identical ambiguous sets,
   * such as the same wildcard-imported exception named in both a throws clause and a call it
   * guards, do compare equal and are treated as handled.)
   *
   * @param type the catch or throws clause type
   * @param exceptionFqns the possible fully-qualified names of the exception type
   * @return true if the type names exactly the exception type
   */
  private boolean typeHandlesException(Type type, Set<String> exceptionFqns) {
    Set<String> typeFqns = fullyQualifiedNameGenerator.getFQNsFromType(type).erasedFqns();
    return typeFqns.equals(exceptionFqns);
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
          method.getFullyQualifiedNames().stream()
              .map(f -> f.substring(f.indexOf('#') + 1))
              .filter(methods::containsKey)
              .findFirst()
              .ifPresent(methodSignature -> methodsToRemove.put(method, methodSignature));
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
   * #addInformation} needs to be called on it. This is used in the initial worklist when some
   * unsolved symbols may not be generated yet to defer additional information processing to a time
   * when all unsolved symbols are generated.
   *
   * @param node The node to query about
   * @return Whether {@link #addInformation} accepts this node
   */
  public boolean needToPostProcess(Node node) {
    return node instanceof ClassOrInterfaceDeclaration
        || node instanceof EnumDeclaration
        || node instanceof MethodDeclaration
        || node instanceof ConstructorDeclaration
        || node instanceof TryStmt
        || node instanceof ThrowStmt
        || node instanceof InstanceOfExpr
        || node instanceof CastExpr
        || node instanceof MethodCallExpr
        || node instanceof TypeParameter
        || node instanceof AssignExpr
        || node instanceof ReturnStmt
        || node instanceof VariableDeclarator
        || node instanceof BinaryExpr
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
   * Shorthand call for {@link #findExistingAndUpdateFQNs(Set)} that takes a {@link
   * FullyQualifiedNameSet} as input.
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
            "Cannot have generated fields/methods before its type is generated. potentialFQNs: "
                + potentialFQNs);
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
          typeArguments,
          fqns.usesGeneratedName());
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
