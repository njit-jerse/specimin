package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithTraversableScope;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserClassDeclaration;
import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;
import org.checkerframework.specimin.JavaParserUtil;

/**
 * Helper class for {@code UnsolvedSymbolGenerator}. This class runs under the assumption that the
 * root nodes are not solvable.
 */
public class FullyQualifiedNameGenerator {
  /**
   * When evaluating an expression, there is only one possible type. However, the location of an
   * expression could vary, depending on the parent classes/interfaces of the class which holds the
   * expression. When determining the location of a {@code FieldAccessExpr}, {@code MethodCallExpr},
   * or {@code NameExpr}, use this method instead of {@code getFQNsForExpressionType} because it
   * doesn't differentiate between different classes, while this method does.
   *
   * <p>For example, take expression a.b where a is of type A. A implements interface B, and
   * interface B extends many different unsolved interfaces C, D, E, F, etc.
   *
   * <p>Thus, a static field b could be in any of the interfaces C, D, E, F, and we need to
   * differentiate between these interfaces.
   *
   * @param expr The expression to do the analysis upon
   * @return A map of simple class names to a set of potential FQNs. Each Map.Entry represents a
   *     different class.
   */
  public static Map<String, Set<String>> getFQNsForExpressionLocation(Expression expr) {
    // Much of this code is very similar to getFQNsForExpressionType. This is because many of
    // these cases only return one possible location; only a few cases differ.
    if (expr.isNameExpr() || (expr.isMethodCallExpr() && !expr.hasScope())) {
      String name = ((NodeWithSimpleName<?>) expr).getNameAsString();

      CompilationUnit cu = expr.findCompilationUnit().get();

      ImportDeclaration staticImport = null;

      for (ImportDeclaration importDecl : cu.getImports()) {
        if (!importDecl.isStatic()) continue;

        if (importDecl.getNameAsString().endsWith("." + name)) {
          staticImport = importDecl;
        }
      }

      if (staticImport != null) {
        String holdingType = staticImport.getName().getQualifier().get().toString();
        return Map.of(
            JavaParserUtil.getSimpleNameFromQualifiedName(holdingType), Set.of(holdingType));
      }

      // If not static, from parent, aka the edge case that makes this method necessary
      return getFQNsOfAllUnresolvableParents(JavaParserUtil.getEnclosingClassLike(expr), expr);
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
      return Map.of();
    }

    if (scope.isSuperExpr()) {
      return getFQNsOfAllUnresolvableParents(JavaParserUtil.getEnclosingClassLike(expr), expr);
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
    // super
    if (expr.isSuperExpr()) {
      return getFQNsFromClassOrInterfaceType(JavaParserUtil.getSuperClass(expr));
    }
    // static class
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
      MethodReferenceExpr methodRef = expr.asMethodReferenceExpr();
      String functionalInterface;

      try {
        // In practice, this may never resolve. JavaParser resolve on MethodReferenceExprs only 
        // resolves if the LHS is also resolvable, which is often not the case for this method.
        // Instead, we'll need to rely on JavaTypeCorrect to give us the correct functional interface.
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

        functionalInterface =
            getNameOfFunctionalInterfaceWithQualifiedParameters(parameters, isVoid);
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
    // lambda
    else if (expr.isLambdaExpr()) {
      LambdaExpr lambda = expr.asLambdaExpr();

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

    // local variable / field / method call / object creation expression / any other case
    // where the expression is resolvable
    // While the field/method itself may not be resolvable, the scope may be.
    // If it's a local variable or field declaration, the scope will be resolvable.
    try {
      Node node = null;
      if (expr instanceof Resolvable<?>) {
        Object resolved = ((Resolvable<?>) expr).resolve();

        if (resolved instanceof AssociableToAST) {
          node = ((AssociableToAST) resolved).toAst().get();
        }
      }

      // Field declaration and variable declaration expressions
      if (node instanceof NodeWithVariables) {
        NodeWithVariables<?> withVariables = (NodeWithVariables<?>) node;

        Type type = withVariables.getElementType();

        return getFQNsFromType(type);
      }
      // methods, new ClassName()
      else if (node instanceof NodeWithType) {
        NodeWithType<?, ?> asVariableDeclarationExpr = (NodeWithType<?, ?>) node;

        Type type = asVariableDeclarationExpr.getType();

        return getFQNsFromType(type);
      }
    } catch (UnsolvedSymbolException ex) {
      // Not a local variable or field
    }

    // cast expression
    if (expr.isCastExpr()) {
      return getFQNsFromClassOrInterfaceType(expr.asCastExpr().getType().asClassOrInterfaceType());
    }

    // Left-hand side is known (i.e. in a variable declarator, or as an argument)
    if (expr.hasParentNode()) {
      Node parentNode = expr.getParentNode().get();

      // Method call, constructor call, super() call
      if (parentNode instanceof NodeWithArguments<?>) {
        NodeWithArguments<?> methodCall = (NodeWithArguments<?>) parentNode;

        int param = methodCall.getArgumentPosition(expr);

        try {
          // All NodeWithArguments<?> are resolvable, aside from EnumConstantDeclaration,
          // but we won't encounter a situation where EnumConstantDeclaration is on the RHS

          // Constructors and methods both are ResolvedMethodLikeDeclaration
          ResolvedMethodLikeDeclaration resolved =
              (ResolvedMethodLikeDeclaration) ((Resolvable<?>) methodCall).resolve();

          ResolvedType paramType = resolved.getParam(param).getType();

          return Set.of(paramType.describe());
        } catch (UnsolvedSymbolException ex) {
          // Argument type is not resolvable; i.e., method is unsolvable
        }
      } else if (parentNode instanceof VariableDeclarator) {
        VariableDeclarator declarator = (VariableDeclarator) parentNode;

        // When the parent is a VariableDeclarator, the child (expr) is on the right hand side
        // The type is on the left hand side
        return getFQNsFromType(declarator.getType());
      } else if (parentNode instanceof AssignExpr) {
        AssignExpr assignment = (AssignExpr) parentNode;

        // We could be on either side of the assignment operator
        // In that case, take the type of the other side
        if (assignment.getTarget().equals(expr)) {
          return getFQNsForExpressionType(assignment.getValue());
        } else if (assignment.getValue().equals(expr)) {
          return getFQNsForExpressionType(assignment.getTarget());
        }
      }
    }

    // field/method located in unsolvable super class, but it's not explicitly marked by
    // super. Therefore, the type of the expression is unknown.
    // nested field
    // Most likely, this is a field/method of a field/method
    if (expr.isNameExpr()) {
      // It could also be a static field
      NameExpr nameExpr = expr.asNameExpr();

      CompilationUnit cu = expr.findCompilationUnit().get();

      ImportDeclaration staticImport = null;

      for (ImportDeclaration importDecl : cu.getImports()) {
        if (!importDecl.isStatic()) continue;

        if (importDecl.getNameAsString().endsWith("." + nameExpr.getNameAsString())) {
          staticImport = importDecl;
        }
      }

      if (staticImport != null) {
        return Set.of(getFQNOfStaticallyImportedFieldType(staticImport.getNameAsString()));
      }

      String exprTypeName = "SyntheticTypeFor" + toCapital(expr.asNameExpr().getNameAsString());
      return getFQNsFromClassName(
          exprTypeName, exprTypeName, expr.findCompilationUnit().get(), null);
    } else if (expr.isFieldAccessExpr()) {
      String exprTypeName =
          "SyntheticTypeFor" + toCapital(expr.asFieldAccessExpr().getNameAsString());
      return getFQNsFromClassName(
          exprTypeName, exprTypeName, expr.findCompilationUnit().get(), null);
    } else if (expr.isMethodCallExpr()) {
      String exprTypeName = toCapital(expr.asMethodCallExpr().getNameAsString()) + "ReturnType";
      return getFQNsFromClassName(
          exprTypeName, exprTypeName, expr.findCompilationUnit().get(), null);
    } else if (expr.isObjectCreationExpr()) {
      return getFQNsFromClassOrInterfaceType(expr.asObjectCreationExpr().getType());
    }

    // Theoretically, we should not be hitting this point, but we may have forgotten to account
    // for a specific case
    throw new RuntimeException("Unknown expression scope type. Please contact developers.");
  }

  private static Set<String> getFQNsFromType(Type type) {
    if (type.isPrimitiveType() || type.isVoidType()) {
      return Set.of(type.toString());
    } else if (type.isClassOrInterfaceType()) {
      return getFQNsFromClassOrInterfaceType(type.asClassOrInterfaceType());
    }
    return Set.of();
  }

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
        getImportedName, type.getNameWithScope(), type.findCompilationUnit().get(), type);
  }

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
      String firstIdentifier, String fullName, CompilationUnit compilationUnit, Node node) {
    Set<String> fqns = new HashSet<>();

    // If a class or interface type is unresolvable, it must be imported or be in the same package.
    for (ImportDeclaration importDecl : compilationUnit.getImports()) {
      if (importDecl.getNameAsString().endsWith("." + firstIdentifier)) {
        fqns.add(importDecl.getNameAsString());
      }
    }

    if (fqns.isEmpty()) {
      // Not imported
      if (JavaParserUtil.isAClassPath(fullName)) {
        // 1) fully qualified name
        fqns.add(fullName);

        // 2) inner class of a parent class (i.e. Map.Entry), which could then fall under 3) and 4)
      }

      // 3) in current package
      Optional<PackageDeclaration> packageDecl = compilationUnit.getPackageDeclaration();
      if (packageDecl.isPresent()) {
        fqns.add(packageDecl.get().getNameAsString() + "." + fullName);
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
    }
    return fqns;
  }

  /**
   * Gets the FQN of the type of a statically imported field.
   *
   * @param fieldExpr the field access expression to be used as input. This field access expression
   *     must be in the form of a qualified class name
   */
  public static String getFQNOfStaticallyImportedFieldType(String fieldExpr) {
    // As this code involves complex string operations, we'll use a field access expression as an
    // example, following its progression through the code.
    // Suppose this is our field access expression: com.example.MyClass.myField
    List<String> fieldParts = Splitter.onPattern("[.]").splitToList(fieldExpr);
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
        .append("SyntheticType");

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
    Map<String, Set<String>> map = new HashMap<>();

    getAllUnresolvableParentsImpl(typeDecl, currentNode, map);

    return map;
  }

  private static void getAllUnresolvableParentsImpl(
      TypeDeclaration<?> typeDecl, Node currentNode, Map<String, Set<String>> map) {
    if (typeDecl instanceof NodeWithImplements<?>) {
      for (ClassOrInterfaceType type : ((NodeWithImplements<?>) typeDecl).getImplementedTypes()) {
        if (type == currentNode) continue;

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
        if (type == currentNode) continue;

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
