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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;
import org.checkerframework.specimin.JavaParserUtil;

/**
 * Helper class for {@link UnsolvedSymbolGenerator}. Generates all FQNs based on an expression or
 * type.
 */
public class FullyQualifiedNameGenerator {
  private static final String SYNTHETIC_TYPE_FOR = "SyntheticTypeFor";
  private static final String RETURN_TYPE = "ReturnType";

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
  public static Map<String, Set<String>> getFQNsForExpressionLocation(Expression expr) {
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
                expr.toString(), getFQNsFromClassName(expr.toString(), expr.toString(), cu, expr));
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
          getFQNsFromClassOrInterfaceType(expr.asObjectCreationExpr().getType()));
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
        NodeList<ClassOrInterfaceType> bound =
            ((TypeParameter) typeParam.toAst().get()).getTypeBound();

        Map<String, Set<String>> potentialFQNs = new LinkedHashMap<>();

        for (ClassOrInterfaceType type : bound) {
          try {
            resolved = type.resolve();

            Optional<ResolvedReferenceTypeDeclaration> optionalTypeDecl =
                resolved.asReferenceType().getTypeDeclaration();

            if (optionalTypeDecl.isPresent() && optionalTypeDecl.get().toAst().isPresent()) {
              TypeDeclaration<?> typeDecl =
                  (TypeDeclaration<?>) optionalTypeDecl.get().toAst().get();

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

          if (potentialFQNs.containsKey(simpleClassName)) {
            potentialFQNs.get(simpleClassName).addAll(getFQNsFromClassOrInterfaceType(type));
          } else {
            potentialFQNs.put(simpleClassName, getFQNsFromClassOrInterfaceType(type));
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

        if (resolvedValueDeclaration.toAst().isPresent()) {
          Node toAst = resolvedValueDeclaration.toAst().get();

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

              Set<String> fqns = getFQNsFromType(type);

              if (fqns.isEmpty()) {
                continue;
              }

              String simple = JavaParserUtil.getSimpleNameFromQualifiedName(fqns.iterator().next());
              result.put(simple, fqns);
            }

            return result;
          }
        }
      } catch (UnsolvedSymbolException ex) {
        // Not a union type since declaration is unresolvable
      }
    }

    // After these cases, we've handled all exceptions where the scope could be various different
    // locations.
    // Non-super members with scope are located in the same type as the scope; there is only one
    // possible type for these scopes.

    Set<String> fqns = getFQNsForExpressionType(scope);
    String simpleName = JavaParserUtil.getSimpleNameFromQualifiedName(fqns.iterator().next());

    return Map.of(simpleName, fqns);
  }

  /**
   * Given an expression, this method returns possible FQNs of its type.
   *
   * @param expr The expression
   * @return The potential FQNs of the type of the given expression.
   */
  public static Set<String> getFQNsForExpressionType(Expression expr) {
    // If the type of the expression can already be calculated, return it
    if (JavaParserUtil.isExprTypeResolvable(expr)) {
      return Set.of(expr.calculateResolvedType().describe());
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

      return getFQNsFromClassName(
          scoped.toString(), expr.toString(), expr.findCompilationUnit().get(), expr);
    }
    // method ref
    else if (expr.isMethodReferenceExpr()) {
      return getFQNsForMethodReferenceType(expr.asMethodReferenceExpr());
    }
    // lambda
    else if (expr.isLambdaExpr()) {
      return getFQNsForLambdaType(expr.asLambdaExpr());
    } else if (expr.isLiteralExpr()) {
      if (expr.isNullLiteralExpr()) {
        // TODO: more robust handling?
        return Set.of("java.lang.Object");
      }

      return Set.of(expr.asLiteralExpr().calculateResolvedType().describe());
    }
    // Special wrapper for method reference scopes
    else if (expr.isTypeExpr()) {
      return getFQNsFromType(expr.asTypeExpr().getType());
    }
    // cast expression
    else if (expr.isCastExpr()) {
      return getFQNsFromClassOrInterfaceType(expr.asCastExpr().getType().asClassOrInterfaceType());
    }

    // local variable / field / method call / object creation expression / any other case
    // where the expression is resolvable BUT the type of the expression may not be.
    try {
      if (expr instanceof Resolvable<?> resolvable) {
        @Nullable Set<String> solvableDeclarationTypeFQNs =
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
      @Nullable Set<String> fromLHS = getFQNsFromSurroundingContextType(expr);
      if (fromLHS != null) {
        return fromLHS;
      }
    }

    // field/method located in unsolvable super class, but it's not explicitly marked by
    // super. It could also be a static member, either statically imported, a static member
    // of an imported class, or a static member of a class in the same package.
    if (expr.isNameExpr()) {
      String name = expr.asNameExpr().getNameAsString();

      CompilationUnit cu = expr.findCompilationUnit().get();

      ImportDeclaration importDecl = getImportDeclarationFromName(name, cu, false);

      if (importDecl != null) {
        // The name expr could also be a class: calling this method on the scope of Baz.foo
        // where Baz is the name expr could mean that it's an imported type and thus static.
        if (!importDecl.isStatic()) {
          return getFQNsFromClassName(
              expr.toString(), expr.toString(), expr.findCompilationUnit().get(), expr);
        }
        return Set.of(getFQNOfStaticallyImportedMemberType(importDecl.getNameAsString(), false));
      }

      String exprTypeName = SYNTHETIC_TYPE_FOR + toCapital(name);
      return getFQNsFromClassName(
          exprTypeName, exprTypeName, expr.findCompilationUnit().get(), null);
    } else if (expr.isFieldAccessExpr()) {
      Expression scope = expr.asFieldAccessExpr().getScope();

      String exprTypeName;
      if (scope.isThisExpr() || scope.isSuperExpr()) {
        exprTypeName = SYNTHETIC_TYPE_FOR + toCapital(expr.asFieldAccessExpr().getNameAsString());
      } else {
        String scopeType =
            getFQNsForExpressionType(expr.asFieldAccessExpr().getScope()).iterator().next();

        return Set.of(
            getFQNOfStaticallyImportedMemberType(
                scopeType + "." + expr.asFieldAccessExpr().getNameAsString(), false));
      }

      // Place in the same package as its scope type
      while (scope.hasScope()) {
        scope = ((NodeWithTraversableScope) scope).traverseScope().get();
      }

      Set<String> fqns = getFQNsForExpressionType(scope);
      Set<String> result = new LinkedHashSet<>();

      for (String fqn : fqns) {
        result.add(fqn.substring(0, fqn.lastIndexOf('.') + 1) + exprTypeName);
      }

      return result;
    } else if (expr.isMethodCallExpr()) {
      String name = expr.asMethodCallExpr().getNameAsString();

      CompilationUnit cu = expr.findCompilationUnit().get();

      ImportDeclaration staticImport = getImportDeclarationFromName(name, cu, false);

      if (staticImport != null) {
        return Set.of(getFQNOfStaticallyImportedMemberType(staticImport.getNameAsString(), true));
      }

      String exprTypeName = toCapital(expr.asMethodCallExpr().getNameAsString()) + RETURN_TYPE;

      if (expr.hasScope()) {
        // Place in the same package as its scope type
        Expression scope = expr;

        while (scope.hasScope()) {
          scope = ((NodeWithTraversableScope) scope).traverseScope().get();
        }

        Set<String> fqns = getFQNsForExpressionType(scope);
        Set<String> result = new LinkedHashSet<>();

        for (String fqn : fqns) {
          result.add(fqn.substring(0, fqn.lastIndexOf('.') + 1) + exprTypeName);
        }

        return result;
      }
      return getFQNsFromClassName(
          exprTypeName, exprTypeName, expr.findCompilationUnit().get(), null);
    } else if (expr.isObjectCreationExpr()) {
      return getFQNsFromClassOrInterfaceType(expr.asObjectCreationExpr().getType());
    }

    // Hitting this error means we forgot to account for a case
    throw new RuntimeException(
        "Unknown expression type: " + expr.getClass() + "; expression value: " + expr);
  }

  /**
   * Given a method reference expression, return the FQNs of its functional interface.
   *
   * @param methodRef The method reference expression
   * @return The FQNs of its functional interface
   */
  private static Set<String> getFQNsForMethodReferenceType(MethodReferenceExpr methodRef) {
    String functionalInterface;

    try {
      // In practice, this may never resolve. JavaParser resolve on MethodReferenceExprs only
      // resolves if the LHS is also resolvable, which is often not the case for this method.
      // TODO: find all possible definitions for this method and return all their functional
      // interfaces
      ResolvedMethodDeclaration resolved = methodRef.resolve();

      // Try to get the most exact parameters; if something is unresolvable and has an ambiguous
      // FQN, then opt to use java.lang.Object; placing alternates into a type parameter would
      // require a lot more code, which we may choose to do in the future.
      List<String> parameters = new ArrayList<>();
      if (resolved.toAst().isPresent()) {
        Node toAst = resolved.toAst().get();

        CallableDeclaration<?> callableDecl = (CallableDeclaration<?>) toAst;

        for (Parameter param : callableDecl.getParameters()) {
          try {
            parameters.add(param.resolve().describeType());
          } catch (UnsolvedSymbolException ex) {
            Set<String> fqns = getFQNsFromType(param.getType());
            if (fqns.size() == 1) {
              parameters.add(fqns.iterator().next());
            } else {
              parameters.add("java.lang.Object");
            }
          }
        }
      } else {
        // By reflection or jar
        for (int i = 0; i < resolved.getNumberOfParams(); i++) {
          try {
            parameters.add(resolved.getParam(i).describeType());
          } catch (UnsolvedSymbolException ex) {
            parameters.add("java.lang.Object");
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

      functionalInterface = getNameOfFunctionalInterfaceWithQualifiedParameters(parameters, isVoid);
      // UnsupportedOperationException is for constructors
    } catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
      if (methodRef.getIdentifier().equals("new")) {
        functionalInterface = getNameOfFunctionalInterface(0, false);
      } else {
        // If the method ref is unresolvable, use a built-in type (Runnable)
        functionalInterface = getNameOfFunctionalInterface(0, true);
      }
    }

    // firstIdentifier is used to check imports. Since functionalInterface is always either
    // java.____ or a simple synthetic class name, we know it's never required to import.
    return getFQNsFromClassName(
        "java", functionalInterface, methodRef.findCompilationUnit().get(), methodRef);
  }

  /**
   * Given a lambda expression, return the FQNs of its functional interface.
   *
   * @param lambda The lambda expression
   * @return The FQNs of its functional interface
   */
  private static Set<String> getFQNsForLambdaType(LambdaExpr lambda) {
    boolean isVoid;

    if (lambda.getExpressionBody().isPresent()) {
      Expression body = lambda.getExpressionBody().get();
      Set<String> fqns = getFQNsForExpressionType(body);
      isVoid = fqns.size() == 1 && fqns.iterator().next().equals("void");
    } else {
      isVoid =
          lambda.getBody().asBlockStmt().getStatements().stream()
              .anyMatch(stmt -> stmt instanceof ReturnStmt);
    }

    List<String> parameters = new ArrayList<>();

    for (Parameter param : lambda.getParameters()) {
      try {
        parameters.add(param.resolve().describeType());
      } catch (UnsolvedSymbolException ex) {
        Set<String> fqns = getFQNsFromType(param.getType());
        if (fqns.size() == 1) {
          parameters.add(fqns.iterator().next());
        } else {
          parameters.add("java.lang.Object");
        }
      }
    }

    String functionalInterface =
        getNameOfFunctionalInterfaceWithQualifiedParameters(parameters, isVoid);

    // firstIdentifier is used to check imports. Since functionalInterface is always either
    // java.____ or a simple synthetic class name, we know it's never required to import.
    return getFQNsFromClassName(
        "java", functionalInterface, lambda.findCompilationUnit().get(), lambda);
  }

  /**
   * Given an expression that can be resolved, return a best shot at its type based on a resolved
   * declaration. May return null if a type cannot be found from the resolved declaration. This
   * method will also throw an UnsolvedSymbolException if .resolve() fails.
   *
   * @param resolvable A resolvable expression
   * @return A set of FQNs, or null if unfound
   */
  private static @Nullable Set<String> getFQNsForTypeOfSolvableExpression(
      Resolvable<?> resolvable) {
    Node node = null;
    Object resolved = resolvable.resolve();

    if (resolved instanceof AssociableToAST) {
      node = ((AssociableToAST) resolved).toAst().get();
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
   * @return A set of FQNs, or null if unfound
   */
  private static @Nullable Set<String> getFQNsFromSurroundingContextType(Expression expr) {
    Node parentNode = expr.getParentNode().get();

    // Method call, constructor call, super() call
    if (parentNode instanceof NodeWithArguments<?>) {
      NodeWithArguments<?> methodCall = (NodeWithArguments<?>) parentNode;

      int param = -1;
      for (int i = 0; i < methodCall.getArguments().size(); i++) {
        if (methodCall.getArgument(i).equals(expr)) {
          param = i;
        }
      }

      if (param != -1) {
        try {
          // All NodeWithArguments<?> are resolvable, aside from EnumConstantDeclaration,
          // but we won't encounter a situation where EnumConstantDeclaration is on the RHS

          // Constructors and methods both are ResolvedMethodLikeDeclaration
          ResolvedMethodLikeDeclaration resolved =
              (ResolvedMethodLikeDeclaration) ((Resolvable<?>) methodCall).resolve();

          if (resolved == null) {
            throw new RuntimeException("Unexpected null resolve() value");
          }

          ResolvedType paramType = resolved.getParam(param).getType();

          return Set.of(paramType.describe());
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
    } else if (parentNode instanceof AssignExpr) {
      AssignExpr assignment = (AssignExpr) parentNode;

      // We could be on either side of the assignment operator
      // In that case, take the type of the other side

      // TODO: StackOverflowError likely here, refactor
      if (assignment.getTarget().equals(expr) && !assignment.getValue().isNullLiteralExpr()) {
        return getFQNsForExpressionType(assignment.getValue());
      } else if (assignment.getValue().equals(expr)
          && !assignment.getTarget().isNullLiteralExpr()) {
        return getFQNsForExpressionType(assignment.getTarget());
      }
    }
    // Check if it's the conditional of an if, while, do, ?:; if so, its type is boolean
    else if (parentNode instanceof NodeWithCondition) {
      NodeWithCondition<?> withCondition = (NodeWithCondition<?>) parentNode;

      if (withCondition.getCondition().equals(expr)) {
        return Set.of("boolean");
      }
    }
    // If it's in a binary expression (i.e., + - / * == != etc.), then set it to the type of the
    // other side,
    // if known
    else if (parentNode instanceof BinaryExpr) {
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
        return Set.of("boolean");
      }
      // ==, !=; we don't know the type, since the types on either side are not necessarily equal
      else if (operator != BinaryExpr.Operator.EQUALS
          && operator != BinaryExpr.Operator.NOT_EQUALS) {
        // Treat all other cases; type on one side is equal to the other
        Set<String> otherType = getFQNsForExpressionType(other);

        // No type known for sure, synthetic; these only work with Java built-in types
        if (otherType.size() > 1) {
          // Try getting the type of the LHS; i.e. if looking at getA() + getB() in String x =
          // getA() + getB();
          otherType = getFQNsForExpressionType(binary);

          if (otherType.size() > 1) {
            // int is safe for all the remaining operators
            return Set.of("int");
          }
        }
        return otherType;
      }
    } else if (parentNode instanceof ReturnStmt) {
      Object ancestor =
          parentNode
              .findAncestor(
                  n -> {
                    return n instanceof MethodDeclaration || n instanceof LambdaExpr;
                  },
                  Node.class)
              .get();

      if (ancestor instanceof MethodDeclaration methodDecl) {
        return getFQNsFromType(methodDecl.getType());
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
  public static Set<String> getFQNsFromType(Type type) {
    if (type.isUnknownType()) {
      // Resolving an unknown type throws an error
      return Set.of();
    }

    if (type.isArrayType()) {
      Set<String> result = new LinkedHashSet<>();
      int arrayLevel = type.asArrayType().getArrayLevel();
      for (String fqn : getFQNsFromType(type.asArrayType().getElementType())) {
        result.add(fqn + "[]".repeat(arrayLevel));
      }

      return result;
    }

    try {
      ResolvedType resolved = type.resolve();

      return Set.of(resolved.describe());
    } catch (UnsolvedSymbolException ex) {
      // continue
    }

    if (type.isClassOrInterfaceType()) {
      return getFQNsFromClassOrInterfaceType(type.asClassOrInterfaceType());
    }
    return Set.of();
  }

  /**
   * Returns the possible FQNs of the type, with generics removed.
   *
   * @param type The type
   * @return A set of possible FQNs
   */
  public static Set<String> getFQNsFromClassOrInterfaceType(ClassOrInterfaceType type) {
    // If a ClassOrInterfaceType is Map.Entry, we need to find the import with java.util.Map, not
    // java.util.Map.Entry.
    // Hence, look for the import with the "earliest" scope (with Map.Entry, this would be Map).
    String getImportedName = type.getNameAsString();

    Optional<ClassOrInterfaceType> scope = type.getScope();

    while (scope.isPresent()) {
      getImportedName = scope.get().getNameAsString();
      scope = scope.get().getScope();
    }

    return getFQNsFromClassName(
        JavaParserUtil.erase(getImportedName),
        JavaParserUtil.erase(type.getNameWithScope()),
        type.findCompilationUnit().get(),
        type);
  }

  /**
   * Gets FQNs of an annotation.
   *
   * @param anno The annotation
   * @return A set of possible FQNs
   */
  public static Set<String> getFQNsFromAnnotation(AnnotationExpr anno) {
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

    return getFQNsFromClassName(
        getImportedName, anno.getNameAsString(), anno.findCompilationUnit().get(), parent);
  }

  /**
   * Utility method for generating all possible fully-qualified names given the leftmost identifier
   * of a class name, the full known name, and the node.
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
  private static Set<String> getFQNsFromClassName(
      String firstIdentifier,
      String fullName,
      CompilationUnit compilationUnit,
      @Nullable Node node) {
    Set<String> fqns = new LinkedHashSet<>();

    // If a class or interface type is unresolvable, it must be imported or be in the same package.
    for (ImportDeclaration importDecl : compilationUnit.getImports()) {
      if (importDecl.getNameAsString().endsWith("." + firstIdentifier)) {
        return Set.of(importDecl.getNameAsString());
      } else if (importDecl.isAsterisk()) {
        fqns.add(importDecl.getNameAsString() + "." + firstIdentifier);
      }
    }

    // Not imported
    boolean shouldAddAfter = false;
    if (JavaParserUtil.isAClassPath(fullName)) {
      if (JavaParserUtil.isAClassName(fullName)) {
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
      TypeDeclaration<?> enclosingType = JavaParserUtil.getEnclosingClassLike(node);

      // Flatten the map: we only care about the value sets
      for (Set<String> set : getFQNsOfAllUnresolvableParents(enclosingType, node).values()) {
        for (String fqn : set) {
          fqns.add(fqn + "." + fullName);
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
  public static String getFQNOfStaticallyImportedMemberType(String expr, boolean isMethod) {
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
        .append(isMethod ? "ReturnType" : "SyntheticType");

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
  public static String getNameOfFunctionalInterfaceWithQualifiedParameters(
      List<String> parameters, boolean isVoid) {
    // check arity:
    int numberOfParams = parameters.size();
    if (numberOfParams == 0 && isVoid) {
      return "java.lang.Runnable";
    } else if (numberOfParams == 0 && !isVoid) {
      return "java.util.function.Supplier<?>";
    } else if (numberOfParams == 1 && isVoid) {
      return "java.util.function.Consumer<" + parameters.get(0) + ">";
    } else if (numberOfParams == 1 && !isVoid) {
      return "java.util.function.Function<" + parameters.get(0) + ", ?>";
    } else if (numberOfParams == 2 && isVoid) {
      return "java.util.function.BiConsumer<" + String.join(", ", parameters) + ">";
    } else if (numberOfParams == 2 && !isVoid) {
      return "java.util.function.BiFunction<" + String.join(", ", parameters) + ", ?>";
    } else {
      String funcInterfaceName =
          isVoid ? "SyntheticConsumer" + numberOfParams : "SyntheticFunction" + numberOfParams;

      StringBuilder typeArgs = new StringBuilder();
      typeArgs.append("<");
      typeArgs.append(String.join(", ", parameters));

      if (isVoid) {
        typeArgs.append(", ?");
      }

      typeArgs.append(">");
      return funcInterfaceName + typeArgs;
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
  public static String getNameOfFunctionalInterface(int numberOfParams, boolean isVoid) {
    return getNameOfFunctionalInterfaceWithQualifiedParameters(
        Collections.nCopies(numberOfParams, "Object"), isVoid);
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
  public static Map<String, Set<String>> getFQNsOfAllUnresolvableParents(
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
  private static void getAllUnresolvableParentsImpl(
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
          map.put(type.getNameWithScope(), getFQNsFromClassOrInterfaceType(type));
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
          map.put(type.getNameWithScope(), getFQNsFromClassOrInterfaceType(type));
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
