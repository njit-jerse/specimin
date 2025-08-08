package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
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
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;
import org.checkerframework.specimin.JavaLangUtils;
import org.checkerframework.specimin.JavaParserUtil;

/**
 * Helper class for {@link UnsolvedSymbolGenerator}. Generates all FQNs based on an expression or
 * type.
 */
public class FullyQualifiedNameGenerator {
  private static final String SYNTHETIC_TYPE_FOR = "SyntheticTypeFor";
  private static final String RETURN_TYPE = "ReturnType";
  private Map<String, CompilationUnit> fqnToCompilationUnits;

  /**
   * Create a new instance. Needs a map of type FQNs to compilation units for symbol resolution.
   *
   * @param fqnToCompilationUnits The map of FQNs to compilation units
   */
  public FullyQualifiedNameGenerator(Map<String, CompilationUnit> fqnToCompilationUnits) {
    this.fqnToCompilationUnits = fqnToCompilationUnits;
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
   * <p>This method may also return an empty map if the method/field is located in a built-in Java
   * class.
   *
   * @param expr The expression to do the analysis upon
   * @return A map of simple class names to a set of potential FQNs. Each Map.Entry represents a
   *     different class. Will return Map.of() if the location is in a solvable type.
   */
  public Map<String, Set<String>> getFQNsForExpressionLocation(Expression expr) {
    if (expr.isNameExpr() || (expr.isMethodCallExpr() && !expr.hasScope())) {
      String name = JavaParserUtil.erase(((NodeWithSimpleName<?>) expr).getNameAsString());

      CompilationUnit cu = expr.findCompilationUnit().get();

      ImportDeclaration staticImport = getImportDeclarationFromName(name, cu, true);

      if (staticImport != null) {
        String holdingType = staticImport.getName().getQualifier().get().toString();
        return Map.of(
            JavaParserUtil.getSimpleNameFromQualifiedName(holdingType), Set.of(holdingType));
      }

      // If not static, from parent, aka the edge case that makes this method necessary
      Map<String, Set<String>> result =
          getFQNsOfAllUnresolvableParents(JavaParserUtil.getEnclosingClassLike(expr), expr);

      if (result.isEmpty() && expr.isNameExpr()) {
        // All parent classes/interfaces are solvable, and do not contain this field. In this case,
        // it's also likely that this could be a type

        result =
            Map.of(
                expr.toString(),
                getFQNsFromErasedClassName(expr.toString(), expr.toString(), cu, expr));
      }

      return result;
    }

    Expression scope;
    if (expr.isFieldAccessExpr()) {
      scope = expr.asFieldAccessExpr().getScope();
    } else if (expr.isMethodCallExpr()) {
      scope = expr.asMethodCallExpr().getScope().get();
    } else if (expr.isObjectCreationExpr()) {
      return Map.of(
          JavaParserUtil.getSimpleNameFromQualifiedName(
              expr.asObjectCreationExpr().getTypeAsString()),
          getFQNsFromClassOrInterfaceType(expr.asObjectCreationExpr().getType()).erasedFqns());
    } else if (expr.isMethodReferenceExpr()) {
      scope = expr.asMethodReferenceExpr().getScope();
    } else {
      throw new RuntimeException(
          "Unexpected call to getFQNsForExpressionLocation with expression type "
              + expr.getClass());
    }

    if (scope.isSuperExpr() || scope.isThisExpr()) {
      return getFQNsOfAllUnresolvableParents(JavaParserUtil.getEnclosingClassLike(expr), expr);
    }

    try {
      ResolvedType resolved = scope.calculateResolvedType();

      if (resolved.isTypeVariable()) {
        ResolvedTypeParameterDeclaration typeParam = resolved.asTypeVariable().asTypeParameter();

        TypeParameter attachedTypeParameter =
            (TypeParameter) JavaParserUtil.findAttachedNode(typeParam, fqnToCompilationUnits);

        NodeList<ClassOrInterfaceType> bound = attachedTypeParameter.getTypeBound();

        if (bound == null) {
          return Map.of();
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

        return potentialFQNs;
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
              return Map.of();
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

          return result;
        }
      } catch (UnsolvedSymbolException ex) {
        // Not a union type since declaration is unresolvable
      }
    }

    // After these cases, we've handled all exceptions where the scope could be various different
    // locations.
    // Non-super members with scope are located in the same type as the scope; there is only one
    // possible type for these scopes.

    Set<String> fqns = getFQNsForExpressionType(scope).erasedFqns();
    String simpleName = JavaParserUtil.getSimpleNameFromQualifiedName(fqns.iterator().next());

    return Map.of(simpleName, fqns);
  }

  /**
   * Given an expression, this method returns possible FQNs of its type. If the type is an array,
   * all FQNs will have the same number of array brackets.
   *
   * @param expr The expression
   * @return The potential FQNs of the type of the given expression.
   */
  public FullyQualifiedNameSet getFQNsForExpressionType(Expression expr) {
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
  public FullyQualifiedNameSet getFQNsForExpressionTypeImpl(Expression expr, boolean canRecurse) {
    // If the type of the expression can already be calculated, return it
    // Throws UnsupportedOperationException for annotation expressions
    if (!expr.isAnnotationExpr() && JavaParserUtil.isExprTypeResolvable(expr)) {
      ResolvedType type = expr.calculateResolvedType();

      return getFQNsForResolvedType(type);
    }

    // super
    if (expr.isSuperExpr()) {
      return getFQNsFromClassOrInterfaceType(JavaParserUtil.getSuperClass(expr));
    }
    // scope of a static field/method
    else if (JavaParserUtil.isAClassPath(expr.toString())) {
      Expression scoped = expr;

      while (scoped instanceof NodeWithTraversableScope
          && ((NodeWithTraversableScope) scoped).traverseScope().isPresent()) {
        scoped = ((NodeWithTraversableScope) scoped).traverseScope().get();
      }

      return new FullyQualifiedNameSet(
          getFQNsFromErasedClassName(
              scoped.toString(), expr.toString(), expr.findCompilationUnit().get(), expr));
    } else if (expr.isNameExpr() && JavaParserUtil.isAClassName(expr.toString())) {
      return new FullyQualifiedNameSet(
          getFQNsFromErasedClassName(
              expr.toString(), expr.toString(), expr.findCompilationUnit().get(), expr));
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
      return getFQNsFromType(expr.asTypeExpr().getType());
    }
    // cast expression
    else if (expr.isCastExpr()) {
      return getFQNsFromClassOrInterfaceType(expr.asCastExpr().getType().asClassOrInterfaceType());
    } else if (expr.isClassExpr()) {
      return new FullyQualifiedNameSet(
          Set.of("java.lang.Class"), List.of(getFQNsFromType(expr.asClassExpr().getType())));
    } else if (expr.isAnnotationExpr()) {
      return getFQNsFromAnnotation(expr.asAnnotationExpr());
    } else if (expr.isArrayAccessExpr()) {
      FullyQualifiedNameSet fqns = getFQNsForExpressionType(expr.asArrayAccessExpr().getName());
      Set<String> result = new LinkedHashSet<>();
      for (String fqn : fqns.erasedFqns()) {
        if (fqn.endsWith("[]")) {
          result.add(fqn.substring(0, fqn.length() - 2));
        }
      }
      return new FullyQualifiedNameSet(result, fqns.typeArguments());
    }

    // local variable / field / method call / object creation expression / any other case
    // where the expression is resolvable BUT the type of the expression may not be.
    try {
      if (expr instanceof Resolvable<?> resolvable) {
        @Nullable FullyQualifiedNameSet solvableDeclarationTypeFQNs =
            getFQNsForTypeOfSolvableExpression(resolvable);
        if (solvableDeclarationTypeFQNs != null) {
          return solvableDeclarationTypeFQNs;
        }
      }
    } catch (UnsolvedSymbolException ex) {
      // Not a local variable or field
    }

    // Handle the cases where the type of the expression can be inferred from surrounding context
    if (expr.hasParentNode()) {
      @Nullable FullyQualifiedNameSet fromLHS = getFQNsFromSurroundingContextType(expr, canRecurse);
      if (fromLHS != null) {
        return fromLHS;
      }
    }

    // Handle binary expressions after surrounding context because the binary expression could be on
    // the
    // right-hand side where the left side type is known.
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
        return new FullyQualifiedNameSet("boolean");
      } else {
        // Treat all other cases; type on one side is equal to the other
        FullyQualifiedNameSet leftType = getFQNsForExpressionTypeImpl(binary.getLeft(), canRecurse);
        FullyQualifiedNameSet rightType =
            getFQNsForExpressionTypeImpl(binary.getRight(), canRecurse);

        // Remaining operators only work with primitive/String types
        // Safe to call isJavaLangOrPrimitiveName since any non-primitive/String type would be
        // a synthetic type here
        if (leftType.erasedFqns().size() == 1
            && JavaLangUtils.isJavaLangOrPrimitiveName(leftType.erasedFqns().iterator().next())) {
          return leftType;
        }
        if (rightType.erasedFqns().size() == 1
            && JavaLangUtils.isJavaLangOrPrimitiveName(rightType.erasedFqns().iterator().next())) {
          return rightType;
        }

        // TODO: alternates here? cases here are all primitives (except for +, which could be
        // String)
        return new FullyQualifiedNameSet("int");
      }
    }

    // field/method located in unsolvable super class, but it's not explicitly marked by
    // super. It could also be a static member, either statically imported, a static member
    // of an imported class, or a static member of a class in the same package.
    String fqnOfStaticMember = JavaParserUtil.getFQNIfStaticMember(expr);
    if (fqnOfStaticMember != null) {
      return new FullyQualifiedNameSet(
          generateFQNForTheTypeOfAStaticallyImportedMember(
              fqnOfStaticMember, expr.isMethodCallExpr()));
    }

    if (expr.isNameExpr()) {
      String name = expr.asNameExpr().getNameAsString();

      CompilationUnit cu = expr.findCompilationUnit().get();

      ImportDeclaration importDecl = getImportDeclarationFromName(name, cu, false);

      if (importDecl != null) {
        // The name expr could also be a class: calling this method on the scope of Baz.foo
        // where Baz is the name expr could mean that it's an imported type and thus static.
        if (!importDecl.isStatic()) {
          return new FullyQualifiedNameSet(
              getFQNsFromErasedClassName(
                  expr.toString(), expr.toString(), expr.findCompilationUnit().get(), expr));
        }
        return new FullyQualifiedNameSet(
            generateFQNForTheTypeOfAStaticallyImportedMember(importDecl.getNameAsString(), false));
      }

      String exprTypeName = SYNTHETIC_TYPE_FOR + toCapital(name);
      return new FullyQualifiedNameSet(
          getFQNsFromErasedClassName(
              exprTypeName, exprTypeName, expr.findCompilationUnit().get(), null));
    } else if (expr.isFieldAccessExpr()) {
      Expression scope = expr.asFieldAccessExpr().getScope();

      String exprTypeName;
      if (scope.isThisExpr() || scope.isSuperExpr()) {
        exprTypeName = SYNTHETIC_TYPE_FOR + toCapital(expr.asFieldAccessExpr().getNameAsString());
      } else {
        String scopeType =
            getFQNsForExpressionType(expr.asFieldAccessExpr().getScope())
                .erasedFqns()
                .iterator()
                .next();

        return new FullyQualifiedNameSet(
            generateFQNForTheTypeOfAStaticallyImportedMember(
                scopeType + "." + expr.asFieldAccessExpr().getNameAsString(), false));
      }

      // Place in the same package as its scope type
      while (scope.hasScope()) {
        scope = ((NodeWithTraversableScope) scope).traverseScope().get();
      }

      Set<String> fqns = getFQNsForExpressionType(scope).erasedFqns();
      Set<String> result = new LinkedHashSet<>();

      for (String fqn : fqns) {
        result.add(fqn.substring(0, fqn.lastIndexOf('.') + 1) + exprTypeName);
      }

      return new FullyQualifiedNameSet(result);
    } else if (expr.isMethodCallExpr()) {
      String exprTypeName = toCapital(expr.asMethodCallExpr().getNameAsString()) + RETURN_TYPE;
      // Place in the same package as its scope type
      Set<String> fqns = getFQNsForExpressionLocation(expr).values().iterator().next();
      Set<String> result = new LinkedHashSet<>();

      for (String fqn : fqns) {
        result.add(fqn.substring(0, fqn.lastIndexOf('.') + 1) + exprTypeName);
      }

      return new FullyQualifiedNameSet(result);
    } else if (expr.isObjectCreationExpr()) {
      return getFQNsFromClassOrInterfaceType(expr.asObjectCreationExpr().getType());
    }

    // Hitting this error means we forgot to account for a case
    throw new RuntimeException(
        "Unknown expression type: " + expr.getClass() + "; expression value: " + expr);
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

      ResolvedReferenceTypeDeclaration typeDecl =
          resolvedType.asReferenceType().getTypeDeclaration().orElse(null);

      if (typeDecl != null && typeDecl.toAst().isPresent()) {
        TypeDeclaration<?> typeDeclaration = (TypeDeclaration<?>) typeDecl.toAst().get();

        if (typeDeclaration.isPrivate()) {
          // If private, then we use java.lang.Object since this method is likely for use by
          // symbols not in the current class.
          qualifiedName = "java.lang.Object";
        }
      }
      return new FullyQualifiedNameSet(
          Set.of(qualifiedName),
          resolvedType.asReferenceType().typeParametersValues().stream()
              .map(this::getFQNsForResolvedType)
              .toList());
    }

    if (resolvedType.isNull()) {
      // For now, return java.lang.Object. TODO: handle null types better
      return new FullyQualifiedNameSet("java.lang.Object");
    }

    return new FullyQualifiedNameSet(resolvedType.describe());
  }

  /**
   * Given a method reference expression, return the FQNs of its functional interface.
   *
   * @param methodRef The method reference expression
   * @return The FQNs of its functional interface
   */
  private FullyQualifiedNameSet getFQNsForMethodReferenceType(MethodReferenceExpr methodRef) {
    FullyQualifiedNameSet functionalInterface;

    try {
      // In practice, this may never resolve. JavaParser resolve on MethodReferenceExprs only
      // resolves if the LHS is also resolvable, which is often not the case for this method.
      // TODO: find all possible definitions for this method and return all their functional
      // interfaces
      ResolvedMethodDeclaration resolved = methodRef.resolve();

      List<FullyQualifiedNameSet> parameters = new ArrayList<>();

      Node attached = JavaParserUtil.tryFindAttachedNode(resolved, fqnToCompilationUnits);
      if (attached != null) {
        CallableDeclaration<?> callableDecl = (CallableDeclaration<?>) attached;

        for (Parameter param : callableDecl.getParameters()) {
          parameters.add(getFQNsFromType(param.getType()));
        }
      } else {
        // By reflection or jar
        for (int i = 0; i < resolved.getNumberOfParams(); i++) {
          try {
            parameters.add(new FullyQualifiedNameSet(resolved.getParam(i).describeType()));
          } catch (UnsolvedSymbolException ex) {
            parameters.add(new FullyQualifiedNameSet("java.lang.Object"));
          }
        }
      }

      // Getting the return type could also cause an unsolved symbol exception, but we only care
      // if it's void or not
      boolean isVoid;
      try {
        isVoid = resolved.getReturnType().isVoid();
      } catch (UnsolvedSymbolException ex) {
        isVoid = false;
      }

      functionalInterface =
          getSimpleNameOfFunctionalInterfaceWithQualifiedParameters(parameters, isVoid);
      // UnsupportedOperationException is for constructors
    } catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
      if (methodRef.getIdentifier().equals("new")) {
        functionalInterface = getSimpleNameOfFunctionalInterface(0, false);
      } else {
        // If the method ref is unresolvable, use a built-in type (Runnable)
        functionalInterface = getSimpleNameOfFunctionalInterface(0, true);
      }
    }

    // firstIdentifier is used to check imports. Since functionalInterface is always either
    // java.____ or a simple synthetic class name, we know it's never required to import.
    return new FullyQualifiedNameSet(
        getFQNsFromErasedClassName(
            "java",
            functionalInterface.erasedFqns().iterator().next(),
            methodRef.findCompilationUnit().get(),
            methodRef),
        functionalInterface.typeArguments());
  }

  /**
   * Given a lambda expression, return the FQNs of its functional interface.
   *
   * @param lambda The lambda expression
   * @return The FQNs of its functional interface
   */
  private FullyQualifiedNameSet getFQNsForLambdaType(LambdaExpr lambda) {
    boolean isVoid;

    if (lambda.getExpressionBody().isPresent()) {
      Expression body = lambda.getExpressionBody().get();
      Set<String> fqns = getFQNsForExpressionType(body).erasedFqns();
      isVoid = fqns.size() == 1 && fqns.iterator().next().equals("void");
    } else {
      isVoid =
          !lambda.getBody().asBlockStmt().getStatements().stream()
              .anyMatch(stmt -> stmt instanceof ReturnStmt);
    }

    FullyQualifiedNameSet functionalInterface =
        getSimpleNameOfFunctionalInterface(lambda.getParameters().size(), isVoid);

    // firstIdentifier is used to check imports. Since functionalInterface is always either
    // java.____ or a simple synthetic class name, we know it's never required to import.
    return new FullyQualifiedNameSet(
        getFQNsFromErasedClassName(
            "java",
            functionalInterface.erasedFqns().iterator().next(),
            lambda.findCompilationUnit().get(),
            lambda),
        functionalInterface.typeArguments());
  }

  /**
   * Given an expression that can be resolved, return a best shot at its type based on a resolved
   * declaration. May return null if a type cannot be found from the resolved declaration. This
   * method will also throw an UnsolvedSymbolException if .resolve() fails.
   *
   * @param resolvable A resolvable expression
   * @return A set of FQNs, or null if unfound
   */
  private @Nullable FullyQualifiedNameSet getFQNsForTypeOfSolvableExpression(
      Resolvable<?> resolvable) {
    Node node = null;
    Object resolved = resolvable.resolve();

    if (resolved instanceof AssociableToAST associableToAST) {
      node = JavaParserUtil.tryFindAttachedNode(associableToAST, fqnToCompilationUnits);
    }

    // Field declaration and variable declaration expressions
    if (node instanceof NodeWithVariables<?> withVariables) {
      Type type = withVariables.getElementType();

      if (!type.isVarType()) {
        // Keep going if var type
        return getFQNsFromType(type);
      }
    }
    // methods, new ClassName()
    else if (node instanceof NodeWithType<?, ?> withType) {
      return getFQNsFromType(withType.getType());
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
  private @Nullable FullyQualifiedNameSet getFQNsFromSurroundingContextType(
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

            return getFQNsForResolvedType(paramType);
          }
        } catch (UnsolvedSymbolException ex) {
          // Argument type is not resolvable; i.e., method is unsolvable
        }
      }
      // scope of the method call, not an argument, continue

    } else if (parentNode instanceof VariableDeclarator) {
      VariableDeclarator declarator = (VariableDeclarator) parentNode;

      // When the parent is a VariableDeclarator, the child (expr) is on the right hand side
      // The type is on the left hand side
      return getFQNsFromType(declarator.getType());
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
    else if (parentNode instanceof NodeWithCondition) {
      NodeWithCondition<?> withCondition = (NodeWithCondition<?>) parentNode;

      if (withCondition.getCondition().equals(expr)) {
        return new FullyQualifiedNameSet("boolean");
      }
    }
    // If it's in a binary expression (i.e., + - / * == != etc.), then set it to the type of the
    // other side, if known
    else if (parentNode instanceof BinaryExpr && canRecurse) {
      BinaryExpr binary = (BinaryExpr) parentNode;
      Operator operator = binary.getOperator();

      Expression other;

      if (binary.getLeft().equals(expr)) {
        other = binary.getRight();
      } else {
        other = binary.getLeft();
      }

      // Boolean
      if (operator == BinaryExpr.Operator.AND || operator == BinaryExpr.Operator.OR) {
        return new FullyQualifiedNameSet("boolean");
      } else {
        // Treat all other cases; type on one side is equal to the other
        FullyQualifiedNameSet otherType = getFQNsForExpressionTypeImpl(other, false);

        // Safe to call isJavaLangOrPrimitiveName since any non-primitive/String type would be
        // a synthetic type here
        if (otherType.erasedFqns().size() == 1
            && JavaLangUtils.isJavaLangOrPrimitiveName(otherType.erasedFqns().iterator().next())) {
          return otherType;
        }

        if (operator != BinaryExpr.Operator.EQUALS && operator != BinaryExpr.Operator.NOT_EQUALS) {
          // ==, != work with any reference types, so we cannot know for certain the types on
          // either side.
          return null;
        }

        // Try getting the type of the LHS; i.e. if looking at getA() + getB() in String x =
        // getA() + getB();
        otherType = getFQNsForExpressionTypeImpl(binary, false);

        if (otherType.erasedFqns().size() > 1) {
          // int is safe for all the remaining operators
          return new FullyQualifiedNameSet("int");
        } else {
          return otherType;
        }
      }
    } else if (parentNode instanceof ReturnStmt) {
      @SuppressWarnings("unchecked")
      Node ancestor =
          parentNode
              .<Node>findAncestor(
                  n -> {
                    return n instanceof MethodDeclaration || n instanceof LambdaExpr;
                  },
                  Node.class)
              .get();

      if (ancestor instanceof MethodDeclaration methodDecl) {
        return getFQNsFromType(methodDecl.getType());
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
        return new FullyQualifiedNameSet(result, notArray.typeArguments());
      }
    }
    return null;
  }

  /**
   * Gets an import declaration based on a simple name, if one exists. Returns null if not found.
   *
   * @param name The simple name; does not contain a period
   * @param cu The compilation unit
   * @param mustBeStatic True if looking only for static imports
   * @return The import declaration, if found; if not, then null
   */
  private static @Nullable ImportDeclaration getImportDeclarationFromName(
      String name, CompilationUnit cu, boolean mustBeStatic) {
    for (ImportDeclaration importDecl : cu.getImports()) {
      if (mustBeStatic && !importDecl.isStatic()) {
        continue;
      }

      if (importDecl.getNameAsString().endsWith("." + name)) {
        return importDecl;
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
    if (type.isUnknownType()) {
      // Resolving an unknown type throws an error
      // Return java.lang.Object since we don't know the type
      return new FullyQualifiedNameSet("java.lang.Object");
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
        return new FullyQualifiedNameSet(Set.of(), List.of(), "?");
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
   * Returns the possible FQNs of the type.
   *
   * @param type The type
   * @return A set of possible FQNs
   */
  public FullyQualifiedNameSet getFQNsFromClassOrInterfaceType(ClassOrInterfaceType type) {
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
        getFQNsFromErasedClassName(
            JavaParserUtil.erase(getImportedName),
            JavaParserUtil.erase(type.getNameWithScope()),
            type.findCompilationUnit().get(),
            type);

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

      if (enclosingType != null) {
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
   * Gets the name of a functional interface type, given a list of qualified parameter types and the
   * presence/absence of a return type. If a java.lang/util class can be used, then a FQN is
   * returned; if not, then the simple class name is returned.
   *
   * @param parameters a list of qualified parameters
   * @param isVoid true iff the method is void
   * @return the fully-qualified name of a functional interface that is in-scope, matches the
   *     specified arity, and the specified voidness
   */
  public static FullyQualifiedNameSet getSimpleNameOfFunctionalInterfaceWithQualifiedParameters(
      List<FullyQualifiedNameSet> parameters, boolean isVoid) {
    // check arity:
    int numberOfParams = parameters.size();
    if (numberOfParams == 0 && isVoid) {
      return new FullyQualifiedNameSet("java.lang.Runnable");
    } else if (numberOfParams == 0 && !isVoid) {
      return new FullyQualifiedNameSet(
          Set.of("java.util.function.Supplier"), List.of(new FullyQualifiedNameSet("?")));
    } else if (numberOfParams == 1 && isVoid) {
      return new FullyQualifiedNameSet(Set.of("java.util.function.Consumer"), parameters);
    } else if (numberOfParams == 1 && !isVoid) {
      return new FullyQualifiedNameSet(
          Set.of("java.util.function.Function"),
          List.of(parameters.get(0), new FullyQualifiedNameSet("?")));
    } else if (numberOfParams == 2 && isVoid) {
      return new FullyQualifiedNameSet(Set.of("java.util.function.BiConsumer"), parameters);
    } else if (numberOfParams == 2 && !isVoid) {
      return new FullyQualifiedNameSet(
          Set.of("java.util.function.BiFunction"),
          List.of(parameters.get(0), parameters.get(1), new FullyQualifiedNameSet("?")));
    } else {
      String funcInterfaceName =
          isVoid ? "SyntheticConsumer" + numberOfParams : "SyntheticFunction" + numberOfParams;

      if (!isVoid) {
        List<FullyQualifiedNameSet> typeArgs = new ArrayList<>(parameters);
        typeArgs.add(new FullyQualifiedNameSet("?"));

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
  public static FullyQualifiedNameSet getSimpleNameOfFunctionalInterface(
      int numberOfParams, boolean isVoid) {
    List<FullyQualifiedNameSet> parameters = new ArrayList<>(numberOfParams);
    for (int i = 0; i < numberOfParams; i++) {
      parameters.add(new FullyQualifiedNameSet("?"));
    }

    return getSimpleNameOfFunctionalInterfaceWithQualifiedParameters(parameters, isVoid);
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

    getAllUnresolvableParentsImpl(typeDecl, currentNode, map);

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
   */
  private void getAllUnresolvableParentsImpl(
      TypeDeclaration<?> typeDecl, Node currentNode, Map<String, Set<String>> map) {
    if (typeDecl instanceof NodeWithImplements<?>) {
      for (ClassOrInterfaceType type : ((NodeWithImplements<?>) typeDecl).getImplementedTypes()) {
        if (type.equals(currentNode)) {
          continue;
        }

        try {
          ResolvedReferenceType resolved = type.resolve().asReferenceType();
          ResolvedReferenceTypeDeclaration resolvedDecl = resolved.getTypeDeclaration().get();

          if (resolvedDecl instanceof JavaParserClassDeclaration) {
            TypeDeclaration<?> parentTypeDecl =
                ((JavaParserClassDeclaration) resolvedDecl).getWrappedNode();
            getAllUnresolvableParentsImpl(parentTypeDecl, currentNode, map);
          }

        } catch (UnsolvedSymbolException ex) {
          map.put(type.getNameWithScope(), getFQNsFromClassOrInterfaceType(type).erasedFqns());
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
          ResolvedReferenceTypeDeclaration resolvedDecl = resolved.getTypeDeclaration().get();

          if (resolvedDecl instanceof JavaParserClassDeclaration) {
            TypeDeclaration<?> parentTypeDecl =
                ((JavaParserClassDeclaration) resolvedDecl).getWrappedNode();
            getAllUnresolvableParentsImpl(parentTypeDecl, currentNode, map);
          }

        } catch (UnsolvedSymbolException ex) {
          map.put(type.getNameWithScope(), getFQNsFromClassOrInterfaceType(type).erasedFqns());
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
