package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.nodeTypes.NodeWithCondition;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithTraversableScope;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;
import org.checkerframework.specimin.JavaLangUtils;
import org.checkerframework.specimin.JavaParserUtil;

/**
 * Helper class for {@link UnsolvedSymbolGenerator}. Generates all FQNs based on an expression or
 * type.
 */
public class FullyQualifiedNameGenerator {
  /** Constant prefix for generated synthetic types. */
  private static final String SYNTHETIC_TYPE_FOR = "SyntheticTypeFor";

  /** Constant suffix for generated return type symbols. */
  private static final String RETURN_TYPE = "ReturnType";

  /** Map of fully qualified names to their corresponding compilation units. */
  private final Map<String, CompilationUnit> fqnToCompilationUnits;

  /** Map of fully qualified names to their generated symbol alternates. */
  private final Map<String, UnsolvedSymbolAlternates<?>> generatedSymbols;

  /**
   * Create a new instance. Needs a map of type FQNs to compilation units for symbol resolution.
   *
   * @param fqnToCompilationUnits The map of FQNs to compilation units
   * @param generatedSymbols The map of FQNs to generated symbols. Should be the same instance used
   *     in UnsolvedSymbolGenerator.
   */
  public FullyQualifiedNameGenerator(
      Map<String, CompilationUnit> fqnToCompilationUnits,
      Map<String, UnsolvedSymbolAlternates<?>> generatedSymbols) {
    this.fqnToCompilationUnits = fqnToCompilationUnits;
    this.generatedSymbols = generatedSymbols;
  }

  /**
   * When evaluating an expression, there is only one possible type. However, the location of an
   * expression could vary, depending on the parent classes/interfaces of the class which holds the
   * expression. This method and {@link #getFQNsForExpressionType(Expression)} return different
   * values; for example, for the method call {@code foo()}, this method could return the class name
   * from a static import or from unsolved super classes. The latter method would return {@code
   * FooReturnType} or its solvable equivalent.
   *
   * <p>For example, take expression a.b where a is of type A. A implements interface B, and
   * interface B extends many different unsolved interfaces C, D, E, F, etc.
   *
   * <p>Thus, a static field b could be in any of the interfaces C, D, E, F, and we need to
   * differentiate between these interfaces.
   *
   * <p>This method may also return an empty map if the method/field is located in a solvable type.
   *
   * @param expr The expression to do the analysis upon
   * @return A collection of sets of FQNs. Each set represents a different type that the
   *     expression's declaration could be located in.
   */
  public Collection<Set<String>> getFQNsForExpressionLocation(Expression expr) {
    Collection<Set<String>> alreadyGenerated =
        getFQNsForExpressionLocationIfRepresentsGenerated(expr);

    if (alreadyGenerated != null) {
      return alreadyGenerated;
    }

    if (expr.isNameExpr() || (expr.isMethodCallExpr() && !expr.hasScope())) {
      String name = JavaParserUtil.erase(((NodeWithSimpleName<?>) expr).getNameAsString());

      CompilationUnit cu = expr.findCompilationUnit().get();

      ImportDeclaration staticImport = JavaParserUtil.getImportDeclaration(name, cu, true);

      if (staticImport != null) {
        String holdingType = staticImport.getName().getQualifier().get().toString();
        return Set.of(Set.of(holdingType));
      }

      // If not static, from parent
      Collection<Set<String>> result =
          getFQNsOfAllUnresolvableParents(JavaParserUtil.getEnclosingClassLike(expr), expr)
              .values();

      if (result.isEmpty() && expr.isNameExpr() && JavaParserUtil.isAClassName(expr.toString())) {
        // All parent classes/interfaces are solvable, and do not contain this field. In this case,
        // it's also likely that this could be a type

        result = Set.of(getFQNsFromErasedClassName(expr.toString(), expr.toString(), cu, expr));
      }

      return result;
    }

    Expression scope;
    if (expr.isFieldAccessExpr()) {
      scope = expr.asFieldAccessExpr().getScope();
    } else if (expr.isMethodCallExpr()) {
      scope = expr.asMethodCallExpr().getScope().get();
    } else if (expr.isObjectCreationExpr()) {
      return Set.of(
          getFQNsFromClassOrInterfaceType(expr.asObjectCreationExpr().getType()).erasedFqns());
    } else if (expr.isMethodReferenceExpr()) {
      scope = expr.asMethodReferenceExpr().getScope();
    } else {
      throw new RuntimeException(
          "Unexpected call to getFQNsForExpressionLocation with expression type "
              + expr.getClass());
    }

    if (scope.isSuperExpr() || scope.isThisExpr()) {
      return getFQNsOfAllUnresolvableParents(JavaParserUtil.getEnclosingClassLike(expr), expr)
          .values();
    }

    try {
      ResolvedType resolved = scope.calculateResolvedType();

      if (resolved.isTypeVariable()) {
        ResolvedTypeParameterDeclaration typeParam = resolved.asTypeVariable().asTypeParameter();

        TypeParameter attachedTypeParameter =
            (TypeParameter) JavaParserUtil.findAttachedNode(typeParam, fqnToCompilationUnits);

        NodeList<ClassOrInterfaceType> bound = attachedTypeParameter.getTypeBound();

        if (bound == null) {
          return Set.of();
        }

        Map<String, Set<String>> potentialFQNs = new LinkedHashMap<>();

        for (ClassOrInterfaceType type : bound) {
          try {
            resolved = type.resolve();

            Optional<ResolvedReferenceTypeDeclaration> optionalTypeDecl =
                resolved.asReferenceType().getTypeDeclaration();

            if (optionalTypeDecl.isPresent() && optionalTypeDecl.get().toAst().isPresent()) {
              TypeDeclaration<?> typeDecl =
                  (TypeDeclaration<?>)
                      JavaParserUtil.getTypeFromQualifiedName(
                          optionalTypeDecl.get().getQualifiedName(), fqnToCompilationUnits);

              if (typeDecl == null) {
                // We shouldn't ever encounter this error. If toAst() returns a non-empty value,
                // then it is in the project
                throw new RuntimeException("Cannot be null here");
              }

              Map<String, Set<String>> toAdd = getFQNsOfAllUnresolvableParents(typeDecl, type);

              for (String key : toAdd.keySet()) {
                if (potentialFQNs.containsKey(key)) {
                  potentialFQNs.get(key).addAll(toAdd.get(key));
                } else {
                  potentialFQNs.put(key, new LinkedHashSet<>(toAdd.get(key)));
                }
              }
            }
          } catch (UnsolvedSymbolException ex) {
            // Type not resolvable
          }

          String simpleClassName = JavaParserUtil.erase(type.getNameAsString());

          // Since we're looking at the location of the expression, the type arguments are not
          // relevant here.
          if (potentialFQNs.containsKey(simpleClassName)) {
            potentialFQNs
                .get(simpleClassName)
                .addAll(getFQNsFromClassOrInterfaceType(type).erasedFqns());
          } else {
            potentialFQNs.put(simpleClassName, getFQNsFromClassOrInterfaceType(type).erasedFqns());
          }
        }

        return potentialFQNs.values();
      }
    } catch (UnsolvedSymbolException ex) {
      // Type not resolvable
    }

    // Handle union types (NameExpr could be an exception capture in a catch clause)
    if (scope.isNameExpr()) {
      try {
        ResolvedValueDeclaration resolvedValueDeclaration = scope.asNameExpr().resolve();

        Node toAst =
            JavaParserUtil.tryFindAttachedNode(resolvedValueDeclaration, fqnToCompilationUnits);

        if (toAst instanceof Parameter param && param.getType().isUnionType()) {
          UnionType unionType = param.getType().asUnionType();

          Map<String, Set<String>> result = new LinkedHashMap<>();

          for (ReferenceType type : unionType.getElements()) {
            try {
              // If a type in the union type is resolvable, the location of the expression will
              // be in a built-in Java superclass. In this case, return an empty map. Follow this
              // reasoning:
              // If a union type is UnsolvedException | NullPointerException, then any method
              // called on the NameExpr
              // representing an exception of this type will be in Exception or Throwable (or
              // NullPointerException if
              // UnsolvedException extended it).

              // TODO: handle a case where a user-defined exception could be solvable but a parent
              // class of that exception is not.
              type.resolve();
              return Set.of();
            } catch (UnsolvedSymbolException ex) {
              // continue
            }

            // Safe to just use erased fqns: member location does not depend on what the type
            // argument is
            Set<String> fqns = getFQNsFromType(type).erasedFqns();

            if (fqns.isEmpty()) {
              continue;
            }

            String simple = JavaParserUtil.getSimpleNameFromQualifiedName(fqns.iterator().next());
            result.put(simple, fqns);
          }

          return result.values();
        }
      } catch (UnsolvedSymbolException ex) {
        // Not a union type since declaration is unresolvable
      }
    }

    // After these cases, we've handled all exceptions where the scope could be various different
    // locations.
    // Non-super members with scope are located in the same type as the scope; there is only one
    // possible type for these scopes.

    Set<FullyQualifiedNameSet> fqnSets = getFQNsForExpressionType(scope);

    Set<Set<String>> result = new LinkedHashSet<>();

    for (FullyQualifiedNameSet fqnSet : fqnSets) {
      result.add(fqnSet.erasedFqns());
    }

    return result;
  }

  /**
   * Gets the location of an expression if its scope is a generated symbol. If the scope is not a
   * generated symbol (or not yet generated), returns null.
   *
   * @param expr The method call or field access expression
   * @return A collection of FQN sets, each set representing a different type, or null if the scope
   *     is not a generated symbol
   */
  private @Nullable Collection<Set<String>> getFQNsForExpressionLocationIfRepresentsGenerated(
      Expression expr) {
    String name;
    Expression scope;

    if (expr.isFieldAccessExpr()) {
      name = expr.asFieldAccessExpr().getNameAsString();
      scope = expr.asFieldAccessExpr().getScope();
    } else if (expr.isMethodCallExpr()) {
      name = expr.asMethodCallExpr().getNameAsString();
      scope = expr.asMethodCallExpr().getScope().orElse(null);
    } else if (expr.isNameExpr()) {
      name = expr.asNameExpr().getNameAsString();
      scope = null;
    } else {
      return null;
    }

    // Static import
    if (!expr.hasScope()) {
      ImportDeclaration importDecl =
          JavaParserUtil.getImportDeclaration(name, expr.findCompilationUnit().get(), true);

      if (importDecl != null) {
        String location = importDecl.getName().getQualifier().get().toString();

        return List.of(Set.of(location));
      }
    } else if (scope instanceof MethodCallExpr scopeAsMethodCall) {
      // Try an overly-generous approach by using both null and java.lang.Object
      Set<String> potentialScopeScopeFQNs =
          generateMethodFQNsWithSideEffect(
              scopeAsMethodCall, getFQNsForExpressionLocation(scopeAsMethodCall), null, true);
      potentialScopeScopeFQNs.addAll(
          generateMethodFQNsWithSideEffect(
              scopeAsMethodCall, getFQNsForExpressionLocation(scopeAsMethodCall), null, false));

      UnsolvedMethodAlternates genMethod =
          (UnsolvedMethodAlternates) findUnsolvedSymbolIfGenerated(potentialScopeScopeFQNs);
      if (genMethod != null) {
        return genMethod.getReturnTypes().stream()
            .map(returnTypes -> returnTypes.getFullyQualifiedNames())
            .toList();
      }
    }
    // Handle FieldAccessExpr/NameExpr together here
    else if (scope instanceof FieldAccessExpr || scope instanceof NameExpr) {
      Set<String> potentialScopeScopeFQNs = new LinkedHashSet<>();

      String fieldName = ((NodeWithSimpleName<?>) scope).getNameAsString();
      for (Set<String> set : getFQNsForExpressionLocation(scope)) {
        for (String potentialScopeFQN : set) {
          potentialScopeScopeFQNs.add(potentialScopeFQN + "#" + fieldName);
        }
      }

      UnsolvedFieldAlternates genField =
          (UnsolvedFieldAlternates) findUnsolvedSymbolIfGenerated(potentialScopeScopeFQNs);
      if (genField != null) {
        return genField.getTypes().stream().map(type -> type.getFullyQualifiedNames()).toList();
      }
    }

    return null;
  }

  /**
   * Given an expression, this method returns possible FQNs of its type. If the type is an array,
   * all FQNs will have the same number of array brackets. Most calls to this method will return a
   * set of length 1; the only cases so far where that is not the case is when multiple methods
   * correspond to the same method reference or if the expression is seen in a BinaryExpr.
   *
   * @param expr The expression
   * @return The potential FQNs of the type of the given expression.
   */
  public Set<FullyQualifiedNameSet> getFQNsForExpressionType(Expression expr) {
    return getFQNsForExpressionTypeImpl(expr, true);
  }

  /**
   * Given an expression, this method returns possible FQNs of its type. This is the implementation
   * for {@link #getFQNsForExpressionLocation(Expression)}; use this method instead to prevent
   * StackOverflowError when recursing.
   *
   * @param expr The expression
   * @param canRecurse Whether or not this method can call itself
   * @return The potential FQNs of the type of the given expression.
   */
  private Set<FullyQualifiedNameSet> getFQNsForExpressionTypeImpl(
      Expression expr, boolean canRecurse) {
    // If the type of the expression can already be calculated, return it
    // Throws UnsupportedOperationException for annotation expressions
    // Handle class expressions separately; their type argument may be a private type, which
    // is handled below.
    if (!expr.isAnnotationExpr()
        && !expr.isClassExpr()
        && JavaParserUtil.isExprTypeResolvable(expr)) {
      ResolvedType type = expr.calculateResolvedType();

      return Set.of(getFQNsForResolvedType(type));
    }

    Set<FullyQualifiedNameSet> alreadyGenerated = getExpressionTypesIfRepresentsGenerated(expr);

    if (alreadyGenerated != null) {
      return alreadyGenerated;
    }

    // super
    if (expr.isSuperExpr()) {
      return Set.of(getFQNsFromClassOrInterfaceType(JavaParserUtil.getSuperClass(expr)));
    }
    // scope of a static field/method
    else if (JavaParserUtil.isAClassPath(expr.toString())) {
      Expression scoped = expr;

      while (scoped instanceof NodeWithTraversableScope
          && ((NodeWithTraversableScope) scoped).traverseScope().isPresent()) {
        scoped = ((NodeWithTraversableScope) scoped).traverseScope().get();
      }

      return Set.of(
          new FullyQualifiedNameSet(
              getFQNsFromErasedClassName(
                  scoped.toString(), expr.toString(), expr.findCompilationUnit().get(), expr)));
    } else if (expr.isNameExpr() && JavaParserUtil.isAClassName(expr.toString())) {
      return Set.of(
          new FullyQualifiedNameSet(
              getFQNsFromErasedClassName(
                  expr.toString(), expr.toString(), expr.findCompilationUnit().get(), expr)));
    }
    // method ref
    else if (expr.isMethodReferenceExpr()) {
      return getFQNsForMethodReferenceType(expr.asMethodReferenceExpr());
    }
    // lambda
    else if (expr.isLambdaExpr()) {
      return getFQNsForLambdaType(expr.asLambdaExpr());
    }
    // Special wrapper for method reference scopes
    else if (expr.isTypeExpr()) {
      return Set.of(getFQNsFromType(expr.asTypeExpr().getType()));
    }
    // cast expression
    else if (expr.isCastExpr()) {
      return Set.of(
          getFQNsFromClassOrInterfaceType(expr.asCastExpr().getType().asClassOrInterfaceType()));
    } else if (expr.isClassExpr()) {
      return Set.of(
          new FullyQualifiedNameSet(
              Set.of("java.lang.Class"),
              List.of(
                  new FullyQualifiedNameSet(
                      getFQNsFromType(expr.asClassExpr().getType()).erasedFqns(),
                      List.of(),
                      "? extends"))));
    } else if (expr.isAnnotationExpr()) {
      return Set.of(getFQNsFromAnnotation(expr.asAnnotationExpr()));
    } else if (expr.isArrayAccessExpr()) {
      Set<FullyQualifiedNameSet> result = new LinkedHashSet<>();
      for (FullyQualifiedNameSet fqns :
          getFQNsForExpressionType(expr.asArrayAccessExpr().getName())) {

        Set<String> fixedFQNs = new LinkedHashSet<>();
        for (String fqn : fqns.erasedFqns()) {
          if (fqn.endsWith("[]")) {
            fixedFQNs.add(fqn.substring(0, fqn.length() - 2));
          }
        }
        result.add(new FullyQualifiedNameSet(fixedFQNs, fqns.typeArguments()));
      }
      return result;
    } else if (expr.isObjectCreationExpr()) {
      return Set.of(getFQNsFromClassOrInterfaceType(expr.asObjectCreationExpr().getType()));
    } else if (expr.isConditionalExpr()) {
      return Stream.concat(
              getFQNsForExpressionType(expr.asConditionalExpr().getThenExpr()).stream(),
              getFQNsForExpressionType(expr.asConditionalExpr().getElseExpr()).stream())
          .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // local variable / field / method call / object creation expression / any other case
    // where the expression is resolvable BUT the type of the expression may not be.
    try {
      @Nullable Set<FullyQualifiedNameSet> solvableDeclarationTypeFQNs =
          getFQNsForTypeOfSolvableExpression(expr);
      if (solvableDeclarationTypeFQNs != null) {
        return solvableDeclarationTypeFQNs;
      }
    } catch (UnsolvedSymbolException ex) {
      @Nullable FullyQualifiedNameSet findableDeclarationTypeFQNs =
          getFQNsForExpressionInAnonymousClass(expr);
      if (findableDeclarationTypeFQNs != null) {
        return Set.of(findableDeclarationTypeFQNs);
      }
      // Not a local variable or field
    }

    // Handle the cases where the type of the expression can be inferred from surrounding context
    if (expr.hasParentNode()) {
      @Nullable Set<FullyQualifiedNameSet> fromLHS =
          getFQNsFromSurroundingContextType(expr, canRecurse);
      if (fromLHS != null) {
        return fromLHS;
      }
    }

    // Handle binary expressions after surrounding context because the binary expression could be on
    // the right-hand side where the left side type is known.
    if (expr.isBinaryExpr()) {
      BinaryExpr binary = expr.asBinaryExpr();
      Operator operator = binary.getOperator();

      // Boolean
      if (operator == Operator.AND
          || operator == Operator.OR
          || operator == Operator.EQUALS
          || operator == Operator.NOT_EQUALS
          || operator == Operator.LESS
          || operator == Operator.GREATER
          || operator == Operator.LESS_EQUALS
          || operator == Operator.GREATER_EQUALS) {
        return Set.of(new FullyQualifiedNameSet("boolean"));
      } else {
        // Treat all other cases; type on one side is equal to the other
        Set<FullyQualifiedNameSet> leftType =
            getFQNsForExpressionTypeImpl(binary.getLeft(), canRecurse);
        Set<FullyQualifiedNameSet> rightType =
            getFQNsForExpressionTypeImpl(binary.getRight(), canRecurse);

        // Remaining operators only work with primitive/boxed/String types
        // Safe to call isJavaLangOrPrimitiveName since any non-primitive/boxed/String type would be
        // a synthetic type here
        if (leftType.size() == 1
            && leftType.iterator().next().erasedFqns().size() == 1
            && JavaLangUtils.isJavaLangOrPrimitiveName(
                leftType.iterator().next().erasedFqns().iterator().next())) {
          return leftType;
        }
        if (rightType.size() == 1
            && rightType.iterator().next().erasedFqns().size() == 1
            && JavaLangUtils.isJavaLangOrPrimitiveName(
                rightType.iterator().next().erasedFqns().iterator().next())) {
          return rightType;
        }

        Set<FullyQualifiedNameSet> result = new LinkedHashSet<>();
        for (String validType : JavaLangUtils.getTypesForOp(operator.asString())) {
          if (JavaParserUtil.isAClassName(validType)) {
            validType = "java.lang." + validType;
          }
          result.add(new FullyQualifiedNameSet(validType));
        }

        return result;
      }
    }

    // field/method located in unsolvable super class, but it's not explicitly marked by
    // super. It could also be a static member, either statically imported, a static member
    // of an imported class, or a static member of a class in the same package.
    String fqnOfStaticMember = JavaParserUtil.getFQNIfStaticMember(expr);
    if (fqnOfStaticMember != null) {
      return Set.of(
          new FullyQualifiedNameSet(
              generateFQNForTheTypeOfAStaticallyImportedMember(
                  fqnOfStaticMember, expr.isMethodCallExpr())));
    }

    if (expr.isNameExpr()) {
      String name = expr.asNameExpr().getNameAsString();

      CompilationUnit cu = expr.findCompilationUnit().get();

      ImportDeclaration importDecl = JavaParserUtil.getImportDeclaration(name, cu, false);

      if (importDecl != null) {
        // The name expr could also be a class: calling this method on the scope of Baz.foo
        // where Baz is the name expr could mean that it's an imported type and thus static.
        if (!importDecl.isStatic()) {
          return Set.of(
              new FullyQualifiedNameSet(
                  getFQNsFromErasedClassName(
                      expr.toString(), expr.toString(), expr.findCompilationUnit().get(), expr)));
        }
        return Set.of(
            new FullyQualifiedNameSet(
                generateFQNForTheTypeOfAStaticallyImportedMember(
                    importDecl.getNameAsString(), false)));
      }

      String exprTypeName = SYNTHETIC_TYPE_FOR + toCapital(name);
      return Set.of(
          new FullyQualifiedNameSet(
              getFQNsFromErasedClassName(
                  exprTypeName, exprTypeName, expr.findCompilationUnit().get(), null)));
    } else if (expr.isFieldAccessExpr()) {
      Expression scope = expr.asFieldAccessExpr().getScope();

      String exprTypeName;
      if (scope.isThisExpr() || scope.isSuperExpr()) {
        exprTypeName = SYNTHETIC_TYPE_FOR + toCapital(expr.asFieldAccessExpr().getNameAsString());
      } else {
        String scopeType =
            getFQNsForExpressionType(expr.asFieldAccessExpr().getScope())
                .iterator()
                .next()
                .erasedFqns()
                .iterator()
                .next();

        return Set.of(
            new FullyQualifiedNameSet(
                generateFQNForTheTypeOfAStaticallyImportedMember(
                    scopeType + "." + expr.asFieldAccessExpr().getNameAsString(), false)));
      }

      // Place in the same package as its scope type
      while (scope.hasScope()) {
        scope = ((NodeWithTraversableScope) scope).traverseScope().get();
      }

      Set<String> fqns = getFQNsForExpressionType(scope).iterator().next().erasedFqns();
      Set<String> result = new LinkedHashSet<>();

      for (String fqn : fqns) {
        result.add(fqn.substring(0, fqn.lastIndexOf('.') + 1) + exprTypeName);
      }

      return Set.of(new FullyQualifiedNameSet(result));
    } else if (expr.isMethodCallExpr()) {
      String exprTypeName = toCapital(expr.asMethodCallExpr().getNameAsString()) + RETURN_TYPE;
      // Place in the same package as its scope type
      Set<String> fqns = getFQNsForExpressionLocation(expr).iterator().next();
      Set<String> result = new LinkedHashSet<>();

      for (String fqn : fqns) {
        result.add(fqn.substring(0, fqn.lastIndexOf('.') + 1) + exprTypeName);
      }

      return Set.of(new FullyQualifiedNameSet(result));
    }

    // Hitting this error means we forgot to account for a case
    throw new RuntimeException(
        "Unknown expression type: " + expr.getClass() + "; expression value: " + expr);
  }

  /**
   * Gets the type of a generated symbol if it exists, or else returns null if the expression type
   * does not have a generated symbol.
   *
   * @param expression The expression to check
   * @return The set of fully qualified name sets if the expression represents a generated symbol,
   *     or null otherwise
   */
  public @Nullable Set<FullyQualifiedNameSet> getExpressionTypesIfRepresentsGenerated(
      Expression expression) {
    if (expression.isMethodCallExpr()) {
      MethodCallExpr methodCall = expression.asMethodCallExpr();
      Collection<Set<String>> potentialScopeFQNs = getFQNsForExpressionLocation(methodCall);
      // Try an overly-generous approach by using both null and java.lang.Object
      Set<String> methodFQNs =
          generateMethodFQNsWithSideEffect(methodCall, potentialScopeFQNs, null, true);
      methodFQNs.addAll(
          generateMethodFQNsWithSideEffect(methodCall, potentialScopeFQNs, null, false));

      UnsolvedMethodAlternates unsolvedMethodAlternates =
          (UnsolvedMethodAlternates) findUnsolvedSymbolIfGenerated(methodFQNs);

      if (unsolvedMethodAlternates != null) {
        return unsolvedMethodAlternates.getReturnTypes().stream()
            .map(this::convertMemberTypeToFQNSet)
            .collect(Collectors.toCollection(LinkedHashSet::new));
      }

      if (!methodFQNs.isEmpty()) {
        // For purposes of seeing if the method is part of Throwable/Exception/RuntimeException,
        // getting the signature of the first is enough since they should all be the same if their
        // declaring
        // type extends it
        String methodSignature = methodFQNs.iterator().next();
        methodSignature = methodSignature.substring(methodSignature.indexOf('#') + 1);

        for (Set<String> declaringTypeFQNs : potentialScopeFQNs) {
          UnsolvedClassOrInterfaceAlternates declaringType =
              (UnsolvedClassOrInterfaceAlternates) findUnsolvedSymbolIfGenerated(declaringTypeFQNs);

          if (declaringType == null) {
            continue;
          }
          if (declaringType.doesExtend(SolvedMemberType.JAVA_LANG_EXCEPTION)
              || declaringType.doesExtend(SolvedMemberType.JAVA_LANG_ERROR)) {
            if (JavaLangUtils.getJavaLangThrowableMethods().containsKey(methodSignature)) {
              return Set.of(
                  new FullyQualifiedNameSet(
                      JavaLangUtils.getJavaLangThrowableMethods().get(methodSignature)));
            }
          }
        }
      }
    } else if (expression.isFieldAccessExpr()) {
      FieldAccessExpr fieldAccess = expression.asFieldAccessExpr();
      Collection<Set<String>> potentialScopeFQNs = getFQNsForExpressionLocation(fieldAccess);
      Set<String> potentialFQNs = new HashSet<>();

      for (Set<String> scopeFQNs : potentialScopeFQNs) {
        for (String fqn : scopeFQNs) {
          potentialFQNs.add(fqn + "#" + fieldAccess.getNameAsString());
        }
      }

      UnsolvedFieldAlternates unsolvedFieldAlternates =
          (UnsolvedFieldAlternates) findUnsolvedSymbolIfGenerated(potentialFQNs);

      if (unsolvedFieldAlternates != null) {
        return unsolvedFieldAlternates.getTypes().stream()
            .map(this::convertMemberTypeToFQNSet)
            .collect(Collectors.toCollection(LinkedHashSet::new));
      }
    } else if (expression.isNameExpr()) {
      NameExpr nameExpr = expression.asNameExpr();
      Collection<Set<String>> potentialScopeFQNs = getFQNsForExpressionLocation(nameExpr);
      Set<String> potentialFQNs = new HashSet<>();

      for (Set<String> scopeFQNs : potentialScopeFQNs) {
        for (String fqn : scopeFQNs) {
          potentialFQNs.add(fqn + "#" + nameExpr.getNameAsString());
        }
      }

      UnsolvedFieldAlternates unsolvedFieldAlternates =
          (UnsolvedFieldAlternates) findUnsolvedSymbolIfGenerated(potentialFQNs);

      if (unsolvedFieldAlternates != null) {
        return unsolvedFieldAlternates.getTypes().stream()
            .map(this::convertMemberTypeToFQNSet)
            .collect(Collectors.toCollection(LinkedHashSet::new));
      }
    } else if (expression.isConditionalExpr()) {
      ConditionalExpr conditionalExpr = expression.asConditionalExpr();
      Collection<Set<String>> potentialScopeFQNs1 =
          getFQNsForExpressionLocation(conditionalExpr.getThenExpr());
      Collection<Set<String>> potentialScopeFQNs2 =
          getFQNsForExpressionLocation(conditionalExpr.getElseExpr());
      Set<String> potentialFQNs = new HashSet<>();

      for (Set<String> scopeFQNs : potentialScopeFQNs1) {
        for (String fqn : scopeFQNs) {
          potentialFQNs.add(fqn + "#" + conditionalExpr.getCondition().toString());
        }
      }
      for (Set<String> scopeFQNs : potentialScopeFQNs2) {
        for (String fqn : scopeFQNs) {
          potentialFQNs.add(fqn + "#" + conditionalExpr.getCondition().toString());
        }
      }

      UnsolvedFieldAlternates unsolvedFieldAlternates =
          (UnsolvedFieldAlternates) findUnsolvedSymbolIfGenerated(potentialFQNs);

      if (unsolvedFieldAlternates != null) {
        return unsolvedFieldAlternates.getTypes().stream()
            .map(this::convertMemberTypeToFQNSet)
            .collect(Collectors.toCollection(LinkedHashSet::new));
      }
    } else if (expression.isMethodReferenceExpr()) {
      MethodReferenceExpr methodRef = expression.asMethodReferenceExpr();
      Collection<Set<String>> potentialScopeFQNs = getFQNsForExpressionLocation(methodRef);

      boolean isConstructor = JavaParserUtil.erase(methodRef.getIdentifier()).equals("new");
      String methodName =
          isConstructor
              ? JavaParserUtil.getSimpleNameFromQualifiedName(methodRef.getScope().toString())
              : JavaParserUtil.erase(methodRef.getIdentifier());

      Set<String> potentialFullyQualifiedNames = new LinkedHashSet<>();

      for (Set<String> set : potentialScopeFQNs) {
        for (String fqn : set) {
          potentialFullyQualifiedNames.add(fqn + "#" + methodName + "(");
        }
      }

      Set<UnsolvedMethodAlternates> generatedMethods = new LinkedHashSet<>();

      for (Entry<String, UnsolvedSymbolAlternates<?>> genSymbolEntry :
          generatedSymbols.entrySet()) {
        if (!(genSymbolEntry.getValue() instanceof UnsolvedMethodAlternates method)) {
          continue;
        }

        String fullyQualifiedMethodName =
            genSymbolEntry.getKey().substring(0, genSymbolEntry.getKey().indexOf('(') + 1);

        if (potentialFullyQualifiedNames.contains(fullyQualifiedMethodName)) {
          generatedMethods.add(method);
        }
      }

      if (!generatedMethods.isEmpty()) {
        Set<FullyQualifiedNameSet> functionalInterfaces = new LinkedHashSet<>();

        for (UnsolvedMethodAlternates method : generatedMethods) {
          for (UnsolvedMethod alternate : method.getAlternates()) {
            List<FullyQualifiedNameSet> parameterTypes = new ArrayList<>();

            if (!method.isStatic() && methodRef.getScope().isTypeExpr()) {
              parameterTypes.add(getFQNsFromType(methodRef.getScope().asTypeExpr().getType()));
            }

            for (MemberType parameter : alternate.getParameterList()) {
              FullyQualifiedNameSet converted = convertMemberTypeToFQNSet(parameter);
              parameterTypes.add(
                  new FullyQualifiedNameSet(
                      converted.erasedFqns(), converted.typeArguments(), "? extends"));
            }

            functionalInterfaces.add(
                getQualifiedNameOfFunctionalInterface(
                    parameterTypes,
                    !isConstructor && alternate.getReturnType().toString().equals("void")));
          }
        }

        if (!functionalInterfaces.isEmpty()) {
          return functionalInterfaces;
        }
      }
    }

    return null;
  }

  /**
   * Gets method FQNs given a method call expression and a collection of sets of potential FQNs
   * (each set represents a different potential declaring type). argumentToParameterPotentialFQNs is
   * passed in and modified as a side effect (argument mapping to potential type FQNs); if this is
   * null, then there is no side effect.
   *
   * @param methodCall The method call expression
   * @param potentialScopeFQNs Potential scope FQNs
   * @param argumentToParameterPotentialFQNs A map of arguments to their type FQNs; pass in null if
   *     no side effect is desired.
   * @param keepNullInsteadOfObject True if you want to use null instead of Object as part of the
   *     signature.
   * @return The set of strings representing the potential FQNs of this method
   */
  public Set<String> generateMethodFQNsWithSideEffect(
      MethodCallExpr methodCall,
      Collection<Set<String>> potentialScopeFQNs,
      @Nullable Map<Expression, Set<FullyQualifiedNameSet>> argumentToParameterPotentialFQNs,
      boolean keepNullInsteadOfObject) {
    List<Set<String>> simpleNames = new ArrayList<>();

    for (Expression argument : methodCall.getArguments()) {
      if (argument.isNullLiteralExpr() && keepNullInsteadOfObject) {
        simpleNames.add(Set.of("null"));
        if (argumentToParameterPotentialFQNs != null) {
          argumentToParameterPotentialFQNs.put(argument, Set.of(new FullyQualifiedNameSet("null")));
        }
        continue;
      }

      Set<FullyQualifiedNameSet> fqns = getFQNsForExpressionType(argument);

      Set<String> simpleNamesOfThisParameterType = new LinkedHashSet<>();
      for (FullyQualifiedNameSet fqnSet : fqns) {
        String first = fqnSet.erasedFqns().iterator().next();
        String simpleName = JavaParserUtil.getSimpleNameFromQualifiedName(first);
        simpleNamesOfThisParameterType.add(simpleName);
      }

      simpleNames.add(simpleNamesOfThisParameterType);
      if (argumentToParameterPotentialFQNs != null) {
        argumentToParameterPotentialFQNs.put(argument, fqns);
      }
    }

    Set<String> potentialFQNs = new LinkedHashSet<>();

    for (List<String> simpleNamesCombo : JavaParserUtil.generateAllCombinations(simpleNames)) {
      for (Set<String> set : potentialScopeFQNs) {
        for (String potentialScopeFQN : set) {
          potentialFQNs.add(
              potentialScopeFQN
                  + "#"
                  + methodCall.getNameAsString()
                  + "("
                  + String.join(", ", simpleNamesCombo)
                  + ")");
        }
      }
    }

    return potentialFQNs;
  }

  /**
   * Converts MemberType to a FullyQualifiedNameSet.
   *
   * @param memberType The member type
   * @return The FQN set
   */
  private FullyQualifiedNameSet convertMemberTypeToFQNSet(MemberType memberType) {
    List<FullyQualifiedNameSet> typeArguments =
        memberType.getTypeArguments().stream().map(this::convertMemberTypeToFQNSet).toList();

    if (memberType instanceof WildcardMemberType wildcard) {
      MemberType bound = wildcard.getBound();
      if (bound != null) {
        FullyQualifiedNameSet boundFQNs = convertMemberTypeToFQNSet(bound);
        boolean isUpperBound = wildcard.isUpperBounded();
        return new FullyQualifiedNameSet(
            boundFQNs.erasedFqns(),
            boundFQNs.typeArguments(),
            isUpperBound ? "? extends" : "? super");
      } else {
        return FullyQualifiedNameSet.UNBOUNDED_WILDCARD;
      }
    }
    return new FullyQualifiedNameSet(memberType.getFullyQualifiedNames(), typeArguments);
  }

  /**
   * Finds an existing unsolved symbol that matches any of the given potential fully qualified
   * names.
   *
   * @param potentialFQNs A set of potential fully qualified names to check against the generated
   *     symbols.
   * @return An existing UnsolvedSymbolAlternates if found; otherwise, null.
   */
  private @Nullable UnsolvedSymbolAlternates<?> findUnsolvedSymbolIfGenerated(
      Set<String> potentialFQNs) {
    UnsolvedSymbolAlternates<?> alreadyGenerated = null;
    for (String potentialFQN : potentialFQNs) {
      alreadyGenerated = generatedSymbols.get(potentialFQN);

      if (alreadyGenerated != null) {
        return alreadyGenerated;
      }
    }
    return null;
  }

  /**
   * Given a resolved type, return the FQNs of its type.
   *
   * @param resolvedType The resolved type
   * @return The FQNs of the type
   */
  public FullyQualifiedNameSet getFQNsForResolvedType(ResolvedType resolvedType) {
    if (resolvedType.isReferenceType()) {
      String qualifiedName = resolvedType.asReferenceType().getQualifiedName();

      if (JavaParserUtil.areTypeOrOuterTypesPrivate(qualifiedName, fqnToCompilationUnits)) {
        // If private, then we use java.lang.Object since this method is likely for use by
        // symbols not in the current class.
        qualifiedName = "java.lang.Object";
      }
      return new FullyQualifiedNameSet(
          Set.of(qualifiedName),
          resolvedType.asReferenceType().typeParametersValues().stream()
              .map(this::getFQNsForResolvedType)
              .toList());
    } else if (resolvedType.isWildcard()) {
      if (resolvedType.asWildcard().isBounded()) {
        FullyQualifiedNameSet bound =
            getFQNsForResolvedType(resolvedType.asWildcard().getBoundedType());
        boolean isUpperBound = resolvedType.asWildcard().isUpperBounded();

        return new FullyQualifiedNameSet(
            bound.erasedFqns(), bound.typeArguments(), isUpperBound ? "? extends" : "? super");
      } else {
        return FullyQualifiedNameSet.UNBOUNDED_WILDCARD;
      }
    }

    if (resolvedType.isNull()) {
      return new FullyQualifiedNameSet("null");
    }

    return new FullyQualifiedNameSet(resolvedType.describe());
  }

  /**
   * Given a method reference expression, return the FQNs of its functional interface.
   *
   * @param methodRef The method reference expression
   * @return The FQNs of its functional interface
   */
  private Set<FullyQualifiedNameSet> getFQNsForMethodReferenceType(MethodReferenceExpr methodRef) {
    // Applicable java.lang.Object methods. Key is method signature, value is return type.
    Set<Entry<String, String>> applicableObjectMethods =
        JavaLangUtils.getJavaLangObjectMethods().entrySet().stream()
            .filter(k -> k.getKey().startsWith(methodRef.getIdentifier()))
            .collect(Collectors.toCollection(LinkedHashSet::new));

    if (!applicableObjectMethods.isEmpty()) {
      Set<FullyQualifiedNameSet> functionalInterfaces = new LinkedHashSet<>();
      for (Entry<String, String> method : applicableObjectMethods) {
        // If the method is applicable, we can use it as a functional interface
        String parameters =
            method
                .getKey()
                .substring(method.getKey().indexOf('(') + 1, method.getKey().lastIndexOf(')'));
        List<FullyQualifiedNameSet> paramList = new ArrayList<>();

        if (methodRef.getScope().isTypeExpr()) {
          FullyQualifiedNameSet nonWildcard =
              getFQNsFromType(methodRef.getScope().asTypeExpr().getType());
          paramList.add(
              new FullyQualifiedNameSet(
                  nonWildcard.erasedFqns(), nonWildcard.typeArguments(), "? extends"));
        }

        for (String param : parameters.split(",\\s*", -1)) {
          if (!param.isEmpty()) {
            paramList.add(new FullyQualifiedNameSet(param));
          }
        }

        functionalInterfaces.add(
            getQualifiedNameOfFunctionalInterface(
                paramList, method.getValue().equals("void"), methodRef));
      }
      return functionalInterfaces;
    }

    Set<FullyQualifiedNameSet> surroundingContextFQNs =
        getFQNsFromSurroundingContextType(methodRef, true);

    if (surroundingContextFQNs != null) {
      return surroundingContextFQNs;
    }

    List<? extends ResolvedMethodLikeDeclaration> declarations =
        JavaParserUtil.getMethodDeclarationsFromMethodRef(methodRef);

    if (!declarations.isEmpty()) {
      Set<FullyQualifiedNameSet> functionalInterfaces = new LinkedHashSet<>();
      for (ResolvedMethodLikeDeclaration resolved : declarations) {
        functionalInterfaces.add(getFunctionalInterfaceForResolvedMethod(methodRef, resolved));
      }

      return functionalInterfaces;
    }

    FullyQualifiedNameSet functionalInterface;
    // If we can't find the method declaration, then pretend it has no parameters (and void if a
    // method)
    if (methodRef.getIdentifier().equals("new")) {
      functionalInterface = getQualifiedNameOfFunctionalInterface(0, false, methodRef);
    } else {
      // If the method ref is unresolvable, use a built-in type (Runnable)
      functionalInterface = getQualifiedNameOfFunctionalInterface(0, true, methodRef);
    }

    return Set.of(functionalInterface);
  }

  /**
   * Gets the functional interface for a resolved method, given a method reference.
   *
   * @param methodRef The method reference expression
   * @param resolved The resolved method-like declaration
   * @return The fully qualified name set representing the functional interface
   */
  public FullyQualifiedNameSet getFunctionalInterfaceForResolvedMethod(
      MethodReferenceExpr methodRef, ResolvedMethodLikeDeclaration resolved) {
    List<FullyQualifiedNameSet> parameters = new ArrayList<>();

    if (methodRef.getScope().isTypeExpr()
        && resolved.isMethod()
        && resolved.asMethod().isStatic()) {
      FullyQualifiedNameSet nonWildcard =
          getFQNsFromType(methodRef.getScope().asTypeExpr().getType());
      parameters.add(
          new FullyQualifiedNameSet(
              nonWildcard.erasedFqns(), nonWildcard.typeArguments(), "? extends"));
    }

    Node attached = JavaParserUtil.tryFindAttachedNode(resolved, fqnToCompilationUnits);
    if (attached != null) {
      CallableDeclaration<?> callableDecl = (CallableDeclaration<?>) attached;

      for (Parameter param : callableDecl.getParameters()) {
        FullyQualifiedNameSet nonWildcard = getFQNsFromType(param.getType());

        if (nonWildcard.erasedFqns().size() == 1
            && nonWildcard.erasedFqns().iterator().next().equals("java.lang.Object")) {
          parameters.add(FullyQualifiedNameSet.UNBOUNDED_WILDCARD);
          continue;
        }

        parameters.add(
            new FullyQualifiedNameSet(
                nonWildcard.erasedFqns(), nonWildcard.typeArguments(), "? extends"));
      }
    } else {
      // By reflection or jar
      for (int i = 0; i < resolved.getNumberOfParams(); i++) {
        try {
          String fqn = resolved.getParam(i).describeType();
          if (fqn.equals("java.lang.Object")) {
            parameters.add(FullyQualifiedNameSet.UNBOUNDED_WILDCARD);
          } else {
            parameters.add(new FullyQualifiedNameSet(fqn));
          }
        } catch (UnsolvedSymbolException ex) {
          parameters.add(FullyQualifiedNameSet.UNBOUNDED_WILDCARD);
        }
      }
    }

    // Getting the return type could also cause an unsolved symbol exception, but we only care
    // if it's void or not
    boolean isVoid;
    try {
      // Constructors are never void
      isVoid =
          resolved instanceof ResolvedMethodDeclaration method
              ? method.getReturnType().isVoid()
              : false;
    } catch (UnsolvedSymbolException ex) {
      isVoid = false;
    }

    return getQualifiedNameOfFunctionalInterface(parameters, isVoid, methodRef);
  }

  /**
   * Given a lambda expression, return the FQNs of its functional interface.
   *
   * @param lambda The lambda expression
   * @return The FQNs of its functional interface
   */
  private Set<FullyQualifiedNameSet> getFQNsForLambdaType(LambdaExpr lambda) {
    boolean isVoid;

    if (lambda.getExpressionBody().isPresent()) {
      Expression body = lambda.getExpressionBody().get();
      Set<String> fqns = getFQNsForExpressionType(body).iterator().next().erasedFqns();
      isVoid = fqns.size() == 1 && fqns.iterator().next().equals("void");
    } else {
      isVoid =
          !lambda.getBody().asBlockStmt().getStatements().stream()
              .anyMatch(stmt -> stmt instanceof ReturnStmt);
    }

    FullyQualifiedNameSet functionalInterface =
        getQualifiedNameOfFunctionalInterface(lambda.getParameters().size(), isVoid, lambda);

    Node body = lambda.getBody();
    List<NameExpr> usedNameExprs = body.findAll(NameExpr.class);

    // List index corresponds with the type argument index, and each set represents potential
    // types to replace these old type arguments (which were just ?)
    List<Set<FullyQualifiedNameSet>> newTypeArguments = new ArrayList<>();

    for (int i = 0; i < functionalInterface.typeArguments().size(); i++) {
      if (i >= lambda.getParameters().size()) {
        // Non-void return types
        newTypeArguments.add(Set.of(functionalInterface.typeArguments().get(i)));
        continue;
      }

      Parameter param = lambda.getParameters().get(i);
      // If the param has no type defined, then we need to use a bounded wildcard
      // so the output program compiles
      if (!param.getType().isUnknownType()) {
        newTypeArguments.add(Set.of(functionalInterface.typeArguments().get(i)));
        continue;
      }

      String paramName = param.getNameAsString();
      NameExpr use =
          usedNameExprs.stream()
              .filter(nameExpr -> nameExpr.getNameAsString().equals(paramName))
              .findFirst()
              .orElse(null);
      if (use != null) {
        Set<FullyQualifiedNameSet> useType = getFQNsForExpressionType(use);
        newTypeArguments.add(
            useType.stream()
                .map(
                    type ->
                        new FullyQualifiedNameSet(
                            type.erasedFqns(), type.typeArguments(), "? extends"))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
      } else {
        newTypeArguments.add(Set.of(functionalInterface.typeArguments().get(i)));
      }
    }

    Set<FullyQualifiedNameSet> result = new LinkedHashSet<>();
    for (List<FullyQualifiedNameSet> combination :
        JavaParserUtil.generateAllCombinations(newTypeArguments)) {
      result.add(new FullyQualifiedNameSet(functionalInterface.erasedFqns(), combination));
    }

    return result;
  }

  /**
   * Given an expression that can be resolved, return a best shot at its type based on a resolved
   * declaration. May return null if a type cannot be found from the resolved declaration. This
   * method will also throw an UnsolvedSymbolException if .resolve() fails.
   *
   * @param expr A resolvable expression
   * @return A set of FQNs, or null if unfound
   */
  private @Nullable Set<FullyQualifiedNameSet> getFQNsForTypeOfSolvableExpression(Expression expr) {
    if (!(expr instanceof Resolvable<?>)) {
      return null;
    }

    Node node = null;
    Object resolved = null;

    try {
      resolved = ((Resolvable<?>) expr).resolve();
    } catch (UnsupportedOperationException ex) {
      resolved =
          JavaParserUtil.tryFindCorrespondingDeclarationForConstraintQualifiedExpression(expr);
    } catch (UnsolvedSymbolException ex) {
      if (expr instanceof NodeWithArguments<?> withArguments) {
        resolved =
            JavaParserUtil.tryFindSingleCallableForNodeWithUnresolvableArguments(
                withArguments, fqnToCompilationUnits);
      }

      if (resolved == null) {
        throw ex;
      }
    }

    if (resolved instanceof AssociableToAST associableToAST) {
      node = JavaParserUtil.tryFindAttachedNode(associableToAST, fqnToCompilationUnits);
    } else if (resolved instanceof Node directlyFoundAst) {
      node = directlyFoundAst;
    }

    // Field declaration and variable declaration expressions
    if (node instanceof NodeWithVariables<?> withVariables) {
      Type type = withVariables.getElementType();
      Expression initializer = null;

      // See if we can find the exact variable, because ElementType gets rid of arrays
      for (VariableDeclarator varDecl : withVariables.getVariables()) {
        if (varDecl.getName().equals(((NodeWithSimpleName<?>) expr).getName())) {
          type = varDecl.getType();
          initializer = varDecl.getInitializer().orElse(null);
          break;
        }
      }

      if (type.isVarType()) {
        if (initializer == null) {
          throw new RuntimeException("Cannot have a var type with no initializer");
        }
        return getFQNsForExpressionType(initializer);
      }

      if (!type.isUnknownType()) {
        // Keep going if var/unknown type
        return Set.of(getFQNsFromType(type));
      }
    }
    // methods, new ClassName()
    else if (node instanceof NodeWithType<?, ?> withType) {
      Type type = withType.getType();
      if (!type.isUnknownType()) {
        // Keep going if unknown type: note this is only possible with Parameter
        return Set.of(getFQNsFromType(type));
      }
    }

    return null;
  }

  /**
   * Given an expression that is in an anonymous class definition, return its FQNs. This method is
   * necessary for fields that are defined within the anonymous class if the parent class is not
   * solvable.
   *
   * @param expr The expression to analyze
   * @return A set of FQNs, or null if unfound
   */
  private @Nullable FullyQualifiedNameSet getFQNsForExpressionInAnonymousClass(Expression expr) {
    // Check if the expression is within the anonymous class. This method is necessary because
    // if the parent class of the anonymous class is not solvable, fields/methods defined within
    // are also unsolvable
    BodyDeclaration<?> decl = JavaParserUtil.tryFindCorrespondingDeclarationInAnonymousClass(expr);

    if (decl == null) {
      return null;
    }

    if (decl.isFieldDeclaration()) {
      for (VariableDeclarator var : decl.asFieldDeclaration().getVariables()) {
        if (var.getName().equals(((NodeWithSimpleName<?>) expr).getName())) {
          return getFQNsFromType(var.getType());
        }
      }
    }
    return null;
  }

  /**
   * Given an expression, try to find its type based on its surrounding context. For example, if an
   * expression is located on the right-hand side of a variable declaration, take the type on the
   * left. If an expression is passed into a known method, return the type of that parameter. This
   * method will return null if the surrounding context's type cannot be found.
   *
   * @param expr The expression
   * @param canRecurse Whether to allow recursion
   * @return A set of FQNs, or null if unfound
   */
  private @Nullable Set<FullyQualifiedNameSet> getFQNsFromSurroundingContextType(
      Expression expr, boolean canRecurse) {
    Node parentNode = expr.getParentNode().get();

    // Method call, constructor call, super() call
    if (parentNode instanceof NodeWithArguments<?>) {
      NodeWithArguments<?> withArguments = (NodeWithArguments<?>) parentNode;

      int param = -1;
      for (int i = 0; i < withArguments.getArguments().size(); i++) {
        if (withArguments.getArgument(i).equals(expr)) {
          param = i;
        }
      }

      if (param != -1) {
        try {
          Object resolved = ((Resolvable<?>) withArguments).resolve();
          // Constructors and methods both are ResolvedMethodLikeDeclaration

          if (resolved instanceof ResolvedMethodLikeDeclaration resolvedMethodLike) {
            ResolvedType paramType = resolvedMethodLike.getParam(param).getType();

            return Set.of(getFQNsForResolvedType(paramType));
          }
        } catch (UnsolvedSymbolException ex) {
          // Argument type is not resolvable; i.e., method is unsolvable
        }

        CallableDeclaration<?> singleCallable =
            JavaParserUtil.tryFindSingleCallableForNodeWithUnresolvableArguments(
                withArguments, fqnToCompilationUnits);
        if (singleCallable != null) {
          return Set.of(getFQNsFromType(singleCallable.getParameter(param).getType()));
        }

        List<? extends CallableDeclaration<?>> allPotentialCallables =
            JavaParserUtil.tryResolveNodeWithUnresolvableArguments(
                withArguments, fqnToCompilationUnits);
        Set<FullyQualifiedNameSet> result = new LinkedHashSet<>();

        for (CallableDeclaration<?> callable : allPotentialCallables) {
          result.add(getFQNsFromType(callable.getParameter(param).getType()));
        }

        if (!result.isEmpty()) {
          return result;
        }
      }
      // scope of the method call, not an argument, continue
    } else if (parentNode instanceof VariableDeclarator) {
      VariableDeclarator declarator = (VariableDeclarator) parentNode;

      // When the parent is a VariableDeclarator, the child (expr) is on the right hand side
      // The type is on the left hand side
      if (!declarator.getType().isVarType()) {
        return Set.of(getFQNsFromType(declarator.getType()));
      }
    } else if (parentNode instanceof AssignExpr && canRecurse) {
      AssignExpr assignment = (AssignExpr) parentNode;

      // We could be on either side of the assignment operator
      // In that case, take the type of the other side

      if (assignment.getTarget().equals(expr) && !assignment.getValue().isNullLiteralExpr()) {
        return getFQNsForExpressionTypeImpl(assignment.getValue(), false);
      } else if (assignment.getValue().equals(expr)
          && !assignment.getTarget().isNullLiteralExpr()) {
        return getFQNsForExpressionTypeImpl(assignment.getTarget(), false);
      }
    }
    // Check if it's the conditional of an if, while, do, ?:; if so, its type is boolean
    else if (parentNode instanceof NodeWithCondition<?> withCondition) {
      if (withCondition.getCondition().equals(expr)) {
        return Set.of(new FullyQualifiedNameSet("boolean"));
      }

      if (withCondition instanceof ConditionalExpr conditionalExpr && canRecurse) {
        Expression other;

        if (conditionalExpr.getThenExpr().equals(expr)) {
          other = conditionalExpr.getElseExpr();
        } else {
          other = conditionalExpr.getThenExpr();
        }

        return getFQNsForExpressionTypeImpl(other, false);
      }
    }
    // If it's in a binary expression (i.e., + - / * == != etc.), then set it to the type of the
    // other side, if known
    else if (parentNode instanceof BinaryExpr binary && canRecurse) {
      Operator operator = binary.getOperator();

      Expression other;

      if (binary.getLeft().equals(expr)) {
        other = binary.getRight();
      } else {
        other = binary.getLeft();
      }

      // Boolean
      if (operator == BinaryExpr.Operator.AND || operator == BinaryExpr.Operator.OR) {
        return Set.of(new FullyQualifiedNameSet("boolean"));
      } else {
        // Treat all other cases; type on one side is equal to the other
        Set<FullyQualifiedNameSet> otherType = getFQNsForExpressionTypeImpl(other, false);

        // Safe to call isJavaLangOrPrimitiveName since any non-primitive/String type would be
        // a synthetic type here
        if (otherType.size() == 1
            && otherType.iterator().next().erasedFqns().size() == 1
            && JavaLangUtils.isJavaLangOrPrimitiveName(
                otherType.iterator().next().erasedFqns().iterator().next())) {
          return otherType;
        }

        if (operator == BinaryExpr.Operator.EQUALS || operator == BinaryExpr.Operator.NOT_EQUALS) {
          // ==, != work with any reference types, so we cannot know for certain the types on
          // either side. Return the synthetic type generated above.
          return otherType;
        }

        // Try getting the type of the LHS; i.e. if looking at getA() + getB() in String x =
        // getA() + getB();
        otherType = getFQNsForExpressionTypeImpl(binary, false);

        if (otherType.size() > 1 || otherType.iterator().next().erasedFqns().size() > 1) {
          Set<FullyQualifiedNameSet> result = new LinkedHashSet<>();
          for (String validType : JavaLangUtils.getTypesForOp(operator.asString())) {
            if (JavaParserUtil.isAClassName(validType)) {
              validType = "java.lang." + validType;
            }
            result.add(new FullyQualifiedNameSet(validType));
          }

          return result;
        } else {
          return otherType;
        }
      }
    } else if (parentNode instanceof ReturnStmt returnStmt) {
      if (JavaParserUtil.findClosestMethodOrLambdaAncestor(returnStmt)
          instanceof MethodDeclaration methodDecl) {
        return Set.of(getFQNsFromType(methodDecl.getType()));
      }
    } else if (parentNode instanceof ForEachStmt) {
      ForEachStmt forEachStmt = (ForEachStmt) parentNode;

      if (forEachStmt.getIterable().equals(expr)) {
        FullyQualifiedNameSet notArray =
            getFQNsFromType(forEachStmt.getVariable().getElementType());

        Set<String> result = new LinkedHashSet<>();
        for (String fqn : notArray.erasedFqns()) {
          result.add(fqn + "[]");
        }
        return Set.of(new FullyQualifiedNameSet(result, notArray.typeArguments()));
      }
    }
    return null;
  }

  /**
   * Gets the FQNs of a type.
   *
   * @param type The type
   * @return A set of FQNs or primitive names.
   */
  public FullyQualifiedNameSet getFQNsFromType(Type type) {
    // Unknown type is a lambda parameter: for example x in x -> (int)x + 1
    if (type.isUnknownType()) {
      throw new RuntimeException("Do not pass in an unknown type to this method.");
    }

    if (type.isArrayType()) {
      Set<String> result = new LinkedHashSet<>();
      int arrayLevel = type.asArrayType().getArrayLevel();
      FullyQualifiedNameSet elementFQNs = getFQNsFromType(type.asArrayType().getElementType());
      for (String fqn : elementFQNs.erasedFqns()) {
        result.add(fqn + "[]".repeat(arrayLevel));
      }

      return new FullyQualifiedNameSet(result, elementFQNs.typeArguments());
    }

    if (type.isWildcardType()) {
      if (type.asWildcardType().getExtendedType().isPresent()) {
        FullyQualifiedNameSet extendedFQNs =
            getFQNsFromType(type.asWildcardType().getExtendedType().get());

        return new FullyQualifiedNameSet(
            extendedFQNs.erasedFqns(), extendedFQNs.typeArguments(), "? extends");
      } else if (type.asWildcardType().getSuperType().isPresent()) {
        FullyQualifiedNameSet superFQNs =
            getFQNsFromType(type.asWildcardType().getSuperType().get());

        return new FullyQualifiedNameSet(
            superFQNs.erasedFqns(), superFQNs.typeArguments(), "? super");
      } else {
        return FullyQualifiedNameSet.UNBOUNDED_WILDCARD;
      }
    }

    try {
      ResolvedType resolved = type.resolve();

      return getFQNsForResolvedType(resolved);
    } catch (UnsolvedSymbolException ex) {
      // continue
    }

    if (type.isClassOrInterfaceType()) {
      return getFQNsFromClassOrInterfaceType(type.asClassOrInterfaceType());
    }

    throw new RuntimeException("Unexpected type: " + type.getClass() + "; type value: " + type);
  }

  /**
   * Returns the possible FQNs of the type. Only use this method with unsolvable types; otherwise,
   * use {@link #getFQNsFromType(Type)}.
   *
   * @param type The type
   * @return A set of possible FQNs
   */
  private FullyQualifiedNameSet getFQNsFromClassOrInterfaceType(ClassOrInterfaceType type) {
    return getFQNsFromClassOrInterfaceTypeImpl(type, Set.of());
  }

  /**
   * Returns the possible FQNs of the type. Only use this method with unsolvable types; otherwise,
   * use {@link #getFQNsFromType(Type)}. Call this instead of {@link
   * #getFQNsFromClassOrInterfaceType(ClassOrInterfaceType)} in {@link
   * #getAllUnresolvableParentsImpl(TypeDeclaration, Node, Map, Set)}.
   *
   * @param type The type
   * @param alreadyTraversed A set of type declarations that have already been traversed to prevent
   *     infinite recursion
   * @return A set of possible FQNs
   */
  private FullyQualifiedNameSet getFQNsFromClassOrInterfaceTypeImpl(
      ClassOrInterfaceType type, Set<TypeDeclaration<?>> alreadyTraversed) {
    // If a ClassOrInterfaceType is Map.Entry, we need to find the import with java.util.Map, not
    // java.util.Map.Entry.
    // Hence, look for the import with the "earliest" scope (with Map.Entry, this would be Map).
    String getImportedName = type.getNameAsString();

    Optional<ClassOrInterfaceType> scope = type.getScope();

    while (scope.isPresent()) {
      getImportedName = scope.get().getNameAsString();
      scope = scope.get().getScope();
    }

    Set<String> erasedFQNs =
        getFQNsFromErasedClassNameImpl(
            JavaParserUtil.erase(getImportedName),
            JavaParserUtil.erase(type.getNameWithScope()),
            type.findCompilationUnit().get(),
            type,
            alreadyTraversed);

    if (type.getTypeArguments().isPresent()) {
      List<FullyQualifiedNameSet> typeArguments = new ArrayList<>();
      for (Type typeArg : type.getTypeArguments().get()) {
        typeArguments.add(getFQNsFromType(typeArg));
      }

      return new FullyQualifiedNameSet(erasedFQNs, typeArguments);
    }

    return new FullyQualifiedNameSet(erasedFQNs);
  }

  /**
   * Gets FQNs of an annotation.
   *
   * @param anno The annotation
   * @return A set of possible FQNs
   */
  public FullyQualifiedNameSet getFQNsFromAnnotation(AnnotationExpr anno) {
    // If an annotation is @Foo.Bar, we need to find the import with org.example.Foo, not
    // org.example.Foo.Bar.
    // Hence, look for the import with the "earliest" scope (with @Foo.Bar, this would be Foo).
    String getImportedName = anno.getNameAsString();

    Optional<Name> scope = anno.getName().getQualifier();

    while (scope.isPresent()) {
      getImportedName = scope.get().asString();
      scope = scope.get().getQualifier();
    }

    TypeDeclaration<?> parent = null;

    if (anno.getParentNode().isPresent()) {
      // Instead of using the annotation, we use its parent. That is because the direct parent of
      // an annotation is the declaration. If we did not use its annotation, the resulting class
      // would not be in scope for the annotation.
      parent = JavaParserUtil.getEnclosingClassLikeOptional(anno.getParentNode().get());
    }

    return new FullyQualifiedNameSet(
        getFQNsFromErasedClassName(
            getImportedName, anno.getNameAsString(), anno.findCompilationUnit().get(), parent));
  }

  /**
   * Utility method for generating all possible fully-qualified names given the leftmost identifier
   * of a class name, the full known name, and the node. Type names must be passed in as their
   * erasures.
   *
   * <p>For example, if the class was Map.Entry (not fully qualified), the leftmost identifier would
   * be Map, the full known name would be Map.Entry, and the node would be the ClassOrInterfaceType
   * holding this value.
   *
   * <p>If node is null, then we will not generate FQNs for this class in unresolvable parent
   * classes.
   *
   * @param firstIdentifier The leftmost identifier of the class name/class path
   * @param fullName The full, known name of the class
   * @param compilationUnit The compilation unit (we need this to be passed in because {@code node}
   *     could be null)
   * @param node The node representing the class (if this is null, we won't look at parent types)
   * @return A set of potential FQNs
   */
  private Set<String> getFQNsFromErasedClassName(
      String firstIdentifier,
      String fullName,
      CompilationUnit compilationUnit,
      @Nullable Node node) {
    return getFQNsFromErasedClassNameImpl(
        firstIdentifier, fullName, compilationUnit, node, Set.of());
  }

  /**
   * Implementation for {@link #getFQNsFromErasedClassName(String, String, CompilationUnit, Node)}.
   * Prevents infinite recursion by not looking at parent types if the parent type declaration has
   * already been visited.
   *
   * @param firstIdentifier The leftmost identifier of the class name/class path
   * @param fullName The full, known name of the class
   * @param compilationUnit The compilation unit (we need this to be passed in because {@code node}
   *     could be null)
   * @param node The node representing the class (if this is null, we won't look at parent types)
   * @param alreadyTraversed A set of type declarations that have already been traversed to prevent
   *     infinite recursion
   * @return A set of potential FQNs
   */
  private Set<String> getFQNsFromErasedClassNameImpl(
      String firstIdentifier,
      String fullName,
      CompilationUnit compilationUnit,
      @Nullable Node node,
      Set<TypeDeclaration<?>> alreadyTraversed) {
    Set<String> fqns = new LinkedHashSet<>();

    // If a class or interface type is unresolvable, it must be imported or be in the same package.
    for (ImportDeclaration importDecl : compilationUnit.getImports()) {
      if (importDecl.getNameAsString().endsWith("." + firstIdentifier)) {
        return Set.of(importDecl.getName().getQualifier().get().toString() + "." + fullName);
      } else if (importDecl.isAsterisk()
          && !JavaLangUtils.inJdkPackage(importDecl.getNameAsString())) {
        fqns.add(importDecl.getNameAsString() + "." + fullName);
      }
    }

    // Not imported
    boolean shouldAddAfter = false;
    if (JavaParserUtil.isAClassPath(fullName)) {
      if (JavaParserUtil.isAClassName(firstIdentifier)) {
        // Likely an inner class of another class, not a fully-qualified name;
        // put the package FQN first so best effort generates that instead
        shouldAddAfter = true;
      } else {
        // 1) fully qualified name
        fqns.add(fullName);
        return fqns;
      }

      // 2) inner class of a parent class (i.e. Map.Entry), which could then fall under 3) and 4)
    }

    // 3) in current package
    Optional<PackageDeclaration> packageDecl = compilationUnit.getPackageDeclaration();
    if (packageDecl.isPresent()) {
      fqns.add(packageDecl.get().getNameAsString() + "." + fullName);

      if (shouldAddAfter) {
        fqns.add(fullName);
      }
    } else {
      fqns.add(fullName);
    }

    if (node != null) {
      // 4) inner class of a parent class of the enclosing class
      TypeDeclaration<?> enclosingType = JavaParserUtil.getEnclosingClassLikeOptional(node);

      if (enclosingType != null && !alreadyTraversed.contains(enclosingType)) {
        // If the node is a ClassOrInterfaceType, find the outermost node, since it could be a
        // generic, which would cause a StackOverflowError
        Node outerNode = node;
        while (outerNode.hasParentNode()
            && outerNode.getParentNode().get() instanceof ClassOrInterfaceType) {
          outerNode = outerNode.getParentNode().get();
        }

        // Flatten the map: we only care about the value sets
        for (Set<String> set : getFQNsOfAllUnresolvableParents(enclosingType, outerNode).values()) {
          for (String fqn : set) {
            fqns.add(fqn + "." + fullName);
          }
        }
      }
    }
    return fqns;
  }

  /**
   * Gets the FQN of the type of a statically imported field/method.
   *
   * @param expr the field access/method call expression to be used as input. Must be in the form of
   *     a qualified class name.
   * @param isMethod true if the expression is a method call, false if it is a field access.
   * @return The fully qualified name of the type of the statically imported member
   */
  public static String generateFQNForTheTypeOfAStaticallyImportedMember(
      String expr, boolean isMethod) {
    // As this code involves complex string operations, we'll use a field access expression as an
    // example, following its progression through the code.
    // Suppose this is our field access expression: com.example.MyClass.myField
    List<String> fieldParts = Splitter.onPattern("[.]").splitToList(expr);
    int numOfFieldParts = fieldParts.size();
    if (numOfFieldParts <= 2) {
      throw new RuntimeException("Not in the form of a statically imported field.");
    }
    // this is the synthetic type of the field
    StringBuilder fieldTypeClassName = new StringBuilder(toCapital(fieldParts.get(0)));
    StringBuilder packageName = new StringBuilder(fieldParts.get(0));
    // According to the above example, fieldName will be myField
    String fieldName = fieldParts.get(numOfFieldParts - 1);
    @SuppressWarnings(
        "signature") // this className is from the second-to-last part of a fully-qualified field
    // signature, which is the simple name of a class. In this case, it is MyClass.
    @ClassGetSimpleName String className = fieldParts.get(numOfFieldParts - 2);
    // After this loop: fieldTypeClassName will be ComExample, and packageName will be com.example
    for (int i = 1; i < numOfFieldParts - 2; i++) {
      fieldTypeClassName.append(toCapital(fieldParts.get(i)));
      packageName.append(".").append(fieldParts.get(i));
    }
    // At this point, fieldTypeClassName will be ComExampleMyClassMyFieldSyntheticType
    fieldTypeClassName
        .append(toCapital(className))
        .append(toCapital(fieldName))
        .append(isMethod ? RETURN_TYPE : "SyntheticType");

    return packageName.toString() + "." + fieldTypeClassName.toString();
  }

  /**
   * Gets the fully qualified name of a functional interface type, given the number of parameters,
   * whether the return type is void, and the context node for resolution.
   *
   * @param numberOfParams the number of parameters the functional interface should have
   * @param isVoid true if the functional interface's method is void, false otherwise
   * @param node any node used to get the compilation unit
   * @return a FullyQualifiedNameSet representing the functional interface type
   */
  public FullyQualifiedNameSet getQualifiedNameOfFunctionalInterface(
      int numberOfParams, boolean isVoid, Node node) {
    FullyQualifiedNameSet simple = getSimpleNameOfFunctionalInterface(numberOfParams, isVoid);

    String name = simple.erasedFqns().iterator().next();
    if (JavaParserUtil.isAClassPath(name)) {
      return simple;
    }

    return new FullyQualifiedNameSet(
        getFQNsFromErasedClassName(name, name, node.findCompilationUnit().get(), node),
        simple.typeArguments());
  }

  /**
   * Gets the fully qualified name of a functional interface type, given a list of qualified
   * parameter types, whether the return type is void, and the context node for resolution.
   *
   * @param parameters a list of fully qualified parameter types
   * @param isVoid true if the functional interface's method is void, false otherwise
   * @param node any node used to get the compilation unit
   * @return a FullyQualifiedNameSet representing the functional interface type
   */
  public FullyQualifiedNameSet getQualifiedNameOfFunctionalInterface(
      List<FullyQualifiedNameSet> parameters, boolean isVoid, Node node) {
    FullyQualifiedNameSet simple = getQualifiedNameOfFunctionalInterface(parameters, isVoid);

    String name = simple.erasedFqns().iterator().next();
    if (JavaParserUtil.isAClassPath(name)) {
      return simple;
    }

    return new FullyQualifiedNameSet(
        getFQNsFromErasedClassName(name, name, node.findCompilationUnit().get(), node),
        simple.typeArguments());
  }

  /**
   * Gets the name of a functional interface type, given a list of qualified parameter types and the
   * presence/absence of a return type. If a java.lang/util class can be used, then a FQN is
   * returned; if not, then the simple class name is returned.
   *
   * @param parameters a list of qualified parameters
   * @param isVoid true iff the method is void
   * @return the fully-qualified name of a functional interface that is in-scope, matches the
   *     specified arity, and the specified voidness
   */
  private static FullyQualifiedNameSet getQualifiedNameOfFunctionalInterface(
      List<FullyQualifiedNameSet> parameters, boolean isVoid) {
    parameters = new ArrayList<>(parameters);

    for (int i = 0; i < parameters.size(); i++) {
      if (parameters.get(i).erasedFqns().size() == 1
          && JavaLangUtils.isPrimitive(parameters.get(i).erasedFqns().iterator().next())) {
        parameters.set(
            i,
            new FullyQualifiedNameSet(
                JavaLangUtils.getPrimitiveAsBoxedType(
                    parameters.get(i).erasedFqns().iterator().next())));
      }
    }

    // check arity:
    int numberOfParams = parameters.size();
    if (numberOfParams == 0 && isVoid) {
      return new FullyQualifiedNameSet("java.lang.Runnable");
    } else if (numberOfParams == 0 && !isVoid) {
      return new FullyQualifiedNameSet(
          Set.of("java.util.function.Supplier"), List.of(FullyQualifiedNameSet.UNBOUNDED_WILDCARD));
    } else if (numberOfParams == 1 && isVoid) {
      return new FullyQualifiedNameSet(Set.of("java.util.function.Consumer"), parameters);
    } else if (numberOfParams == 1 && !isVoid) {
      return new FullyQualifiedNameSet(
          Set.of("java.util.function.Function"),
          List.of(parameters.get(0), FullyQualifiedNameSet.UNBOUNDED_WILDCARD));
    } else if (numberOfParams == 2 && isVoid) {
      return new FullyQualifiedNameSet(Set.of("java.util.function.BiConsumer"), parameters);
    } else if (numberOfParams == 2 && !isVoid) {
      return new FullyQualifiedNameSet(
          Set.of("java.util.function.BiFunction"),
          List.of(parameters.get(0), parameters.get(1), FullyQualifiedNameSet.UNBOUNDED_WILDCARD));
    } else {
      String funcInterfaceName =
          isVoid ? "SyntheticConsumer" + numberOfParams : "SyntheticFunction" + numberOfParams;

      if (!isVoid) {
        List<FullyQualifiedNameSet> typeArgs = new ArrayList<>(parameters);
        typeArgs.add(FullyQualifiedNameSet.UNBOUNDED_WILDCARD);

        parameters = typeArgs;
      }

      return new FullyQualifiedNameSet(Set.of(funcInterfaceName), parameters);
    }
  }

  /**
   * Gets the name of a functional interface type, given the number of parameters and the
   * presence/absence of a return type. If a java.lang/util class can be used, then a FQN is
   * returned; if not, then the simple class name is returned.
   *
   * @param numberOfParams the number of parameters
   * @param isVoid true iff the method is void
   * @return the fully-qualified name of a functional interface that is in-scope, matches the
   *     specified arity, and the specified voidness
   */
  private static FullyQualifiedNameSet getSimpleNameOfFunctionalInterface(
      int numberOfParams, boolean isVoid) {
    List<FullyQualifiedNameSet> parameters = new ArrayList<>(numberOfParams);
    for (int i = 0; i < numberOfParams; i++) {
      parameters.add(FullyQualifiedNameSet.UNBOUNDED_WILDCARD);
    }

    return getQualifiedNameOfFunctionalInterface(parameters, isVoid);
  }

  /**
   * Gets all FQNs of parents (such as implements and extends) recursively, given a type
   * declaration.
   *
   * @param typeDecl the type declaration
   * @param currentNode the current node. If the current node is found to be one of the
   *     implemented/extended types, then we will not go down that path.
   * @return A map of all class/interface FQNs representing all {@code typeDecl}'s parents; simple
   *     class name --> set of potential FQNs
   */
  public Map<String, Set<String>> getFQNsOfAllUnresolvableParents(
      TypeDeclaration<?> typeDecl, Node currentNode) {
    Map<String, Set<String>> map = new LinkedHashMap<>();
    Set<TypeDeclaration<?>> traversedTypeDeclarations = new HashSet<>();

    getAllUnresolvableParentsImpl(typeDecl, currentNode, map, traversedTypeDeclarations);

    return map;
  }

  /**
   * Helper method for {@link #getFQNsOfAllUnresolvableParents(TypeDeclaration, Node)}. This method
   * recursively calls itself on resolvable class/interface declarations, and continues to add fqns
   * to the map.
   *
   * @param typeDecl The type declaration to find parents from
   * @param currentNode The current node, to determine whether or not we should continue down that
   *     path (if node.equals(currentNode), do not do recurse)
   * @param map The map to add to
   * @param traversedTypeDeclarations A set of type declarations that have already been traversed to
   *     avoid infinite recursion
   */
  private void getAllUnresolvableParentsImpl(
      TypeDeclaration<?> typeDecl,
      Node currentNode,
      Map<String, Set<String>> map,
      Set<TypeDeclaration<?>> traversedTypeDeclarations) {
    if (traversedTypeDeclarations.contains(typeDecl)) {
      return;
    }
    traversedTypeDeclarations.add(typeDecl);

    if (typeDecl instanceof NodeWithImplements<?>) {
      for (ClassOrInterfaceType type : ((NodeWithImplements<?>) typeDecl).getImplementedTypes()) {
        if (type.equals(currentNode)) {
          continue;
        }

        try {
          ResolvedReferenceType resolved = type.resolve().asReferenceType();
          TypeDeclaration<?> parentTypeDecl =
              JavaParserUtil.getTypeFromQualifiedName(
                  resolved.getQualifiedName(), fqnToCompilationUnits);

          if (parentTypeDecl != null) {
            getAllUnresolvableParentsImpl(
                parentTypeDecl, currentNode, map, traversedTypeDeclarations);
          }

        } catch (UnsolvedSymbolException ex) {
          map.put(
              type.getNameWithScope(),
              getFQNsFromClassOrInterfaceTypeImpl(type, traversedTypeDeclarations).erasedFqns());
        }
      }
    }

    if (typeDecl instanceof NodeWithExtends<?>) {
      for (ClassOrInterfaceType type : ((NodeWithExtends<?>) typeDecl).getExtendedTypes()) {
        if (type.equals(currentNode)) {
          continue;
        }

        try {
          ResolvedReferenceType resolved = type.resolve().asReferenceType();
          TypeDeclaration<?> parentTypeDecl =
              JavaParserUtil.getTypeFromQualifiedName(
                  resolved.getQualifiedName(), fqnToCompilationUnits);

          if (parentTypeDecl != null) {
            getAllUnresolvableParentsImpl(
                parentTypeDecl, currentNode, map, traversedTypeDeclarations);
          }

        } catch (UnsolvedSymbolException ex) {
          map.put(
              type.getNameWithScope(),
              getFQNsFromClassOrInterfaceTypeImpl(type, traversedTypeDeclarations).erasedFqns());
        }
      }
    }
  }

  /**
   * This method capitalizes a string. For example, "hello" will become "Hello".
   *
   * @param string the string to be capitalized
   * @return the capitalized version of the string
   */
  private static String toCapital(String string) {
    return Ascii.toUpperCase(string.substring(0, 1)) + string.substring(1);
  }
}
