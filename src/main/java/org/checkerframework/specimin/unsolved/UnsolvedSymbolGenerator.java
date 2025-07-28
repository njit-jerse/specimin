package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.PatternExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.specimin.JavaLangUtils;
import org.checkerframework.specimin.JavaParserUtil;

public class UnsolvedSymbolGenerator {
  /**
   * The slice of unsolved symbol definitions. These values need not be unique; the map is provided
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
   * in addition to any types in its scope. However, field types, method return types, parameter
   * types, filed/method holding types are not included in the result list because they can be
   * accessed through UnsolvedFieldAlternates or UnsolvedMethodAlternates.
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
      try {
        asType.resolve();
        return;
      } catch (UnsolvedSymbolException ex) {
        // Ok to continue
      }

      Set<String> potentialFQNs =
          FullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(asType);

      UnsolvedClassOrInterfaceAlternates generated =
          findExistingAndUpdateFQNsOrCreateNewType(potentialFQNs);

      if (asType.getTypeArguments().isPresent()) {
        generated.setNumberOfTypeVariables(asType.getTypeArguments().get().size());
      }

      result.add(generated);
    } else if (node instanceof AnnotationExpr asAnno) {
      try {
        asAnno.resolve();
        return;
      } catch (UnsolvedSymbolException ex) {
        // Ok to continue
      }
      Set<String> potentialFQNs = FullyQualifiedNameGenerator.getFQNsFromAnnotation(asAnno);

      UnsolvedClassOrInterfaceAlternates generated =
          findExistingAndUpdateFQNsOrCreateNewType(potentialFQNs);
      generated.setIsAnAnnotationToTrue();

      result.add(generated);
    }
    // Fields (although types are handled first conditional in FieldAccessExpr)
    else if (node instanceof FieldAccessExpr asField) {
      // When we have a FieldAccessExpr like a.b.c, the scope a.b is also a FieldAccessExpr
      // We need to handle the case where the scope could be a class, like org.example.MyClass,
      // because resolving the scope of a static field like org.example.MyClass.a would return
      // another FieldAccessExpr, not a ClassOrInterfaceType
      if (JavaParserUtil.isAClassPath(node.toString())) {
        Set<String> potentialFQNs = FullyQualifiedNameGenerator.getFQNsForExpressionType(asField);

        UnsolvedClassOrInterfaceAlternates generated =
            findExistingAndUpdateFQNsOrCreateNewType(potentialFQNs);

        result.add(generated);
        return;
      }

      // It may be solvable (when passed into this method via scope)
      // In this case, while the declaration may be solvable, the type may not be
      try {
        ResolvedValueDeclaration resolved = asField.resolve();

        Type type = JavaParserUtil.getTypeFromResolvedValueDeclaration(resolved);

        if (type != null) {
          inferContextImpl(type, result);
        }
      } catch (UnsolvedSymbolException ex) {
        // Ignore
      }

      try {
        asField.calculateResolvedType();

        return;
      } catch (UnsolvedSymbolException ex) {
        // Ignore
      }

      Expression scope = asField.getScope();

      // Generate everything in the scopes before
      inferContextImpl(scope, result);

      Map<String, Set<String>> potentialScopeFQNs =
          FullyQualifiedNameGenerator.getFQNsForExpressionLocation(asField);

      if (potentialScopeFQNs.isEmpty()) {
        return;
      }

      Set<String> potentialFQNs = new HashSet<>();

      for (Set<String> set : potentialScopeFQNs.values()) {
        for (String potentialScopeFQN : set) {
          potentialFQNs.add(potentialScopeFQN + "#" + asField.getNameAsString());
        }
      }

      UnsolvedSymbolAlternates<?> alreadyGenerated = findExistingAndUpdateFQNs(potentialFQNs);

      if (!(alreadyGenerated instanceof UnsolvedFieldAlternates)) {
        Set<String> potentialTypeFQNs =
            FullyQualifiedNameGenerator.getFQNsForExpressionType(asField);

        // Since we called inferContextImpl(scope), the field's parents are created
        List<UnsolvedClassOrInterfaceAlternates> potentialParents = new ArrayList<>();
        for (Set<String> set : potentialScopeFQNs.values()) {
          UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(set);

          if (generated == null) {
            throw new RuntimeException("Field scope types are not yet created");
          }
          potentialParents.add((UnsolvedClassOrInterfaceAlternates) generated);
        }

        MemberType type = getMemberTypeFromFQNs(potentialTypeFQNs);

        if (type.isUnsolved()) {
          result.add(type.getUnsolvedType());
        }

        boolean isStatic = JavaParserUtil.isAStaticMember(asField);

        UnsolvedFieldAlternates field =
            UnsolvedFieldAlternates.create(
                asField.getNameAsString(), type, potentialParents, isStatic, false);

        addNewSymbolToGeneratedSymbolsMap(field);
        result.add(field);
      }
    } else if (node instanceof NameExpr nameExpr) {
      // 1) resolvable (when passed into this method via scope)
      // In this case, while the declaration may be solvable, the type may not be
      try {
        ResolvedValueDeclaration resolved = nameExpr.resolve();

        Type type = JavaParserUtil.getTypeFromResolvedValueDeclaration(resolved);

        if (type != null) {
          inferContextImpl(type, result);
        }

        return;
      } catch (UnsolvedSymbolException ex) {
        // Ignore
      }

      try {
        nameExpr.calculateResolvedType();

        return;
      } catch (UnsolvedSymbolException ex) {
        // Ignore
      }

      // 2) static import
      ImportDeclaration importDecl =
          getNameOfImport(nameExpr.getNameAsString(), node.findCompilationUnit().get());

      if (importDecl != null) {
        if (importDecl.isStatic()) {
          Name staticImport = importDecl.getName();
          String staticImportFqn =
              staticImport.getQualifier().get() + "#" + staticImport.getIdentifier();

          String encapsulatingClass = staticImport.getQualifier().get().toString();

          UnsolvedSymbolAlternates<?> generatedField =
              findExistingAndUpdateFQNs(Set.of(staticImportFqn));

          if (!(generatedField instanceof UnsolvedFieldAlternates)) {
            // Generate/find the class that will hold the field
            UnsolvedClassOrInterfaceAlternates generatedClass =
                findExistingAndUpdateFQNsOrCreateNewType(Set.of(encapsulatingClass));

            // Generate the synthetic type
            String typeFQN =
                FullyQualifiedNameGenerator.getFQNOfStaticallyImportedMemberType(
                    encapsulatingClass, false);

            UnsolvedClassOrInterfaceAlternates generatedType =
                findExistingAndUpdateFQNsOrCreateNewType(Set.of(typeFQN));

            generatedField =
                UnsolvedFieldAlternates.create(
                    staticImport.getIdentifier(),
                    new MemberType(generatedType),
                    List.of(generatedClass),
                    true,
                    true);

            addNewSymbolToGeneratedSymbolsMap(generatedField);

            result.add(generatedField);
          }
        } else {
          result.add(
              findExistingAndUpdateFQNsOrCreateNewType(
                  FullyQualifiedNameGenerator.getFQNsForExpressionType(nameExpr)));
        }

        return;
      }

      // 3) super class field
      // Unresolvable + not a static import --> must be in unsolved parent class
      Map<String, Set<String>> parentClassFQNs =
          FullyQualifiedNameGenerator.getFQNsForExpressionLocation(nameExpr);
      Set<String> fieldFQNs = new HashSet<>();

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

        result.addAll(generatedClasses);

        // Generate the synthetic type
        Set<String> typeFQNs = FullyQualifiedNameGenerator.getFQNsForExpressionType(nameExpr);

        MemberType type = null;
        for (String potentialTypeFQN : typeFQNs) {
          type = getMemberTypeIfPrimitiveOrJavaLang(potentialTypeFQN);

          if (type != null) break;
        }

        if (type == null) {
          UnsolvedClassOrInterfaceAlternates generatedType =
              findExistingAndUpdateFQNsOrCreateNewType(typeFQNs);

          type = new MemberType(generatedType);
          result.add(generatedType);
        }

        generatedField =
            UnsolvedFieldAlternates.create(
                nameExpr.getNameAsString(), type, generatedClasses, false, false);

        addNewSymbolToGeneratedSymbolsMap(generatedField);

        result.add(generatedField);
      }
    }
    // Methods
    else if (node instanceof MethodCallExpr methodCall) {
      try {
        ResolvedMethodDeclaration resolvedMethodDeclaration = methodCall.resolve();

        // If we're here, this was probably passed in as scope or a method argument
        if (resolvedMethodDeclaration.toAst().isPresent()) {
          MethodDeclaration toAst = (MethodDeclaration) resolvedMethodDeclaration.toAst().get();

          inferContextImpl(toAst.getType(), result);
        }

        return;
      } catch (UnsolvedSymbolException ex) {
        // continue
      }

      // A collection of sets of fqns. Each set represents potentially a different class/interface.
      Collection<Set<String>> potentialScopeFQNs = null;

      String methodName = methodCall.getNameAsString();

      // Static import
      if (!methodCall.hasScope()) {
        ImportDeclaration importDecl =
            getNameOfImport(methodName, node.findCompilationUnit().get());

        if (importDecl != null && importDecl.isStatic()) {
          potentialScopeFQNs = Set.of(Set.of(importDecl.getName().getQualifier().get().toString()));

          result.add(
              findExistingAndUpdateFQNsOrCreateNewType(potentialScopeFQNs.iterator().next()));
        }
      } else {
        inferContextImpl(methodCall.getScope().get(), result);
      }

      if (potentialScopeFQNs == null) {
        potentialScopeFQNs =
            FullyQualifiedNameGenerator.getFQNsForExpressionLocation(methodCall).values();
      }

      if (potentialScopeFQNs.isEmpty()) {
        return;
      }

      Map<Expression, Set<String>> argumentToParameterPotentialFQNs = new HashMap<>();

      List<String> simpleNames = new ArrayList<>();

      for (Expression argument : methodCall.getArguments()) {
        Set<String> fqns = FullyQualifiedNameGenerator.getFQNsForExpressionType(argument);
        String first = fqns.iterator().next();
        String simpleName = JavaParserUtil.getSimpleNameFromQualifiedName(first);
        simpleNames.add(simpleName);
        argumentToParameterPotentialFQNs.put(argument, fqns);
      }

      Set<String> potentialFQNs = new HashSet<>();

      for (Set<String> set : potentialScopeFQNs) {
        for (String potentialScopeFQN : set) {
          potentialFQNs.add(
              potentialScopeFQN + "#" + methodName + "(" + String.join(", ", simpleNames) + ")");
        }
      }

      UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(potentialFQNs);
      UnsolvedMethodAlternates generatedMethod;

      if (generated instanceof UnsolvedMethodAlternates) {
        generatedMethod = (UnsolvedMethodAlternates) generated;
      } else {
        Set<String> potentialReturnTypeFQNs =
            FullyQualifiedNameGenerator.getFQNsForExpressionType(methodCall);

        List<UnsolvedClassOrInterfaceAlternates> potentialParents = new ArrayList<>();
        for (Set<String> set : potentialScopeFQNs) {
          UnsolvedSymbolAlternates<?> gen = findExistingAndUpdateFQNs(set);

          if (gen == null) {
            throw new RuntimeException("Method scope types are not yet created");
          }
          potentialParents.add((UnsolvedClassOrInterfaceAlternates) gen);
        }

        MemberType returnType = getMemberTypeFromFQNs(potentialReturnTypeFQNs);

        if (returnType.isUnsolved()) {
          result.add(returnType.getUnsolvedType());
        }

        List<MemberType> parameters = new ArrayList<>();

        for (Expression argument : methodCall.getArguments()) {
          inferContextImpl(argument, result);

          Set<String> set = argumentToParameterPotentialFQNs.get(argument);

          // This null check is just to satisfy the error checker
          if (set == null) {
            throw new RuntimeException("Expected non-null when this is null");
          }

          MemberType paramType = getMemberTypeFromFQNs(set);
          if (paramType.isUnsolved()) {
            result.add(paramType.getUnsolvedType());
          }

          parameters.add(paramType);
        }

        generatedMethod =
            UnsolvedMethodAlternates.create(methodName, returnType, potentialParents, parameters);

        addNewSymbolToGeneratedSymbolsMap(generatedMethod);

        if (methodCall.getTypeArguments().isPresent()) {
          generatedMethod.setNumberOfTypeVariables(methodCall.getTypeArguments().get().size());
        }
      }

      if (JavaParserUtil.isAStaticMember(methodCall)) {
        generatedMethod.setIsStaticToTrue();
      }

      result.add(generatedMethod);
    } else if (node instanceof ObjectCreationExpr
        || node instanceof ExplicitConstructorInvocationStmt) {
      UnsolvedClassOrInterfaceAlternates scope;
      String constructorName;
      List<Expression> arguments;
      int numberOfTypeParams = 0;

      // Calling inferContextButDoNotAddInformation on a ClassOrInterfaceType always returns a list
      // of length 1 with the generated/found type
      if (node instanceof ObjectCreationExpr) {
        ObjectCreationExpr constructor = (ObjectCreationExpr) node;

        inferContextImpl(constructor.getType(), result);
        // Do not generate here; that should be taken care of in the inferContextImpl call above.
        scope =
            (UnsolvedClassOrInterfaceAlternates)
                findExistingAndUpdateFQNs(
                    FullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(
                        constructor.getType()));

        constructorName = constructor.getTypeAsString();
        arguments = constructor.getArguments();

        if (constructor.getTypeArguments().isPresent()) {
          numberOfTypeParams = constructor.getTypeArguments().get().size();
        }
      } else {
        ExplicitConstructorInvocationStmt constructor = (ExplicitConstructorInvocationStmt) node;

        // If it's unresolvable, it's a constructor in the unsolved parent class
        TypeDeclaration<?> decl = JavaParserUtil.getEnclosingClassLike(node);
        if (decl instanceof NodeWithExtends && !constructor.isThis()) {
          // There can only be one extends in a class
          ClassOrInterfaceType superClass = ((NodeWithExtends<?>) decl).getExtendedTypes(0);

          inferContextImpl(superClass, result);
          // Do not generate here; that should be taken care of in the inferContextImpl call above.
          scope =
              (UnsolvedClassOrInterfaceAlternates)
                  findExistingAndUpdateFQNs(
                      FullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(superClass));

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

      Map<Expression, String> argumentToSimpleName = new HashMap<>();
      Map<String, Set<String>> simpleNameToParameterPotentialFQNs = new HashMap<>();

      for (Expression argument : arguments) {
        Set<String> fqns = FullyQualifiedNameGenerator.getFQNsForExpressionType(argument);
        String first = fqns.iterator().next();
        String simpleName = JavaParserUtil.getSimpleNameFromQualifiedName(first);
        simpleNameToParameterPotentialFQNs.put(simpleName, fqns);
        argumentToSimpleName.put(argument, simpleName);
      }

      Set<String> potentialFQNs = new HashSet<>();

      for (String potentialScopeFQN : scope.getFullyQualifiedNames()) {
        potentialFQNs.add(
            potentialScopeFQN
                + "#"
                + constructorName
                + "("
                + String.join(", ", simpleNameToParameterPotentialFQNs.keySet())
                + ")");
      }

      UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(potentialFQNs);
      UnsolvedMethodAlternates generatedMethod;

      if (generated instanceof UnsolvedMethodAlternates) {
        generatedMethod = (UnsolvedMethodAlternates) generated;
      } else {
        List<MemberType> parameters = new ArrayList<>();

        for (Expression argument : arguments) {
          inferContextImpl(argument, result);

          String simpleName = argumentToSimpleName.get(argument);

          // This null check is just to satisfy the error checker
          if (simpleName == null) {
            throw new RuntimeException("Expected non-null when this is null");
          }

          Set<String> set = simpleNameToParameterPotentialFQNs.get(simpleName);

          // This null check is just to satisfy the error checker
          if (set == null) {
            throw new RuntimeException("Expected non-null when this is null");
          }

          MemberType paramType = getMemberTypeFromFQNs(set);
          if (paramType.isUnsolved()) {
            result.add(paramType.getUnsolvedType());
          }

          parameters.add(paramType);
        }

        generatedMethod =
            UnsolvedMethodAlternates.create(
                constructorName, new MemberType(""), List.of(scope), parameters);

        addNewSymbolToGeneratedSymbolsMap(generatedMethod);

        generatedMethod.setNumberOfTypeVariables(numberOfTypeParams);
      }

      result.add(generatedMethod);
    }
    // Method references
    else if (node instanceof MethodReferenceExpr methodRef) {
      boolean isResolvable = false;
      try {
        methodRef.resolve();
        isResolvable = true;
      } catch (UnsolvedSymbolException ex) {
        // Ignored
      }
      // In practice, MethodReferenceExpr may never resolve. JavaParser resolve on
      // MethodReferenceExprs
      // only resolves if the LHS is also resolvable, which is often not the case in this method.
      // Instead,
      // we'll need to rely on JavaTypeCorrect to give us the correct functional interface.

      // TODO: it may also be possible to generate alternates based on all the definitions for a
      // method
      // reference expression. For example, obj::foo could refer to foo(int) or foo(int, String).
      if (!isResolvable) {
        Map<String, Set<String>> potentialScopeFQNs =
            FullyQualifiedNameGenerator.getFQNsForExpressionLocation(methodRef);
        // A method ref has a scope, so its location is known to be a single type; therefore, it's
        // safe to do this.
        String simpleClassName = potentialScopeFQNs.keySet().iterator().next();
        Set<String> scopeFQNs = potentialScopeFQNs.get(simpleClassName);

        Set<String> potentialFQNs = new HashSet<>();

        String methodName = JavaParserUtil.erase(methodRef.getIdentifier());

        boolean isConstructor = methodName.equals("new");

        List<UnsolvedClassOrInterfaceAlternates> scope = new ArrayList<>();
        // Unsolvable method ref: default to parameterless
        if (isConstructor) {
          for (String fqn : scopeFQNs) {
            potentialFQNs.add(fqn + "#" + simpleClassName + "()");
          }
        } else {
          for (String fqn : scopeFQNs) {
            potentialFQNs.add(fqn + "#" + methodName + "()");
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
                  new MemberType(isConstructor ? "" : "void"),
                  scope,
                  parameters);

          if (methodRef.getTypeArguments().isPresent()) {
            generatedMethod.setNumberOfTypeVariables(methodRef.getTypeArguments().get().size());
          }

          addNewSymbolToGeneratedSymbolsMap(generatedMethod);
        }

        result.add(generatedMethod);
      }
    }
    // A lambda expr is not of type Resolvable<?>, but it could be passed into this method
    // when an argument is a lambda.

    // This is a confusing pattern, but the else if of MethodReferenceExpr above also checks to see
    // if
    // it is resolvable. If node is a method reference expression, it will also be resolvable here.
    if (node instanceof LambdaExpr || node instanceof MethodReferenceExpr) {
      int arity;
      boolean isVoid;

      if (node instanceof LambdaExpr) {
        LambdaExpr lambda = (LambdaExpr) node;

        if (lambda.getExpressionBody().isPresent()) {
          Expression body = lambda.getExpressionBody().get();
          Set<String> fqns = FullyQualifiedNameGenerator.getFQNsForExpressionType(body);
          isVoid = fqns.size() == 1 && fqns.iterator().next().equals("void");
        } else {
          isVoid =
              lambda.getBody().asBlockStmt().getStatements().stream()
                  .anyMatch(stmt -> stmt instanceof ReturnStmt);
        }

        arity = lambda.getParameters().size();
      } else {
        MethodReferenceExpr methodRef = (MethodReferenceExpr) node;

        // If we're here, methodRef is, surprisingly, resolvable
        ResolvedMethodDeclaration resolved = methodRef.resolve();

        arity = resolved.getNumberOfParams();

        // Getting the return type could also cause an unsolved symbol exception, but we only care
        // if it's void or not
        try {
          isVoid = resolved.getReturnType().isVoid();
        } catch (UnsolvedSymbolException ex) {
          isVoid = false;
        }
      }

      Set<String> unerasedPotentialFQNs =
          FullyQualifiedNameGenerator.getFQNsForExpressionType((Expression) node);
      Set<String> erasedPotentialFQNs = new HashSet<>();

      for (String unerased : unerasedPotentialFQNs) {
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
        params.add(new MemberType(paramArray[i]));
      }

      String returnType = isVoid ? "void" : "T" + arity;
      UnsolvedMethodAlternates apply =
          UnsolvedMethodAlternates.create(
              "apply", new MemberType(returnType), List.of(functionalInterface), params);

      addNewSymbolToGeneratedSymbolsMap(apply);

      result.add(apply);
    }
  }

  /**
   * Call this method on each node to gather more information on potential unsolved symbols. Call
   * this method AFTER all unsolved symbols are generated.
   *
   * @param node The node to gather more information from
   * @return An object of type {@link AddInformationResult}, usually empty, but the close()
   *     method(s) if first time confirmation of an AutoCloseable, or if the return type is updated
   *     in a method call expression.
   */
  public AddInformationResult addInformation(Node node) {
    List<UnsolvedSymbolAlternates<?>> toAdd = new ArrayList<>();
    List<UnsolvedSymbolAlternates<?>> toRemove = new ArrayList<>();

    if (node instanceof ClassOrInterfaceDeclaration decl) {
      for (ClassOrInterfaceType implemented : decl.getImplementedTypes()) {
        UnsolvedClassOrInterfaceAlternates syntheticType =
            (UnsolvedClassOrInterfaceAlternates)
                findExistingAndUpdateFQNs(
                    FullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(implemented));

        if (syntheticType != null) {
          syntheticType.setIsAnInterfaceToTrue();
        }
      }
    } else if (node instanceof MethodDeclaration methodDecl) {
      for (ReferenceType thrownException : methodDecl.getThrownExceptions()) {
        if (!thrownException.isClassOrInterfaceType()) continue;

        UnsolvedClassOrInterfaceAlternates syntheticType =
            (UnsolvedClassOrInterfaceAlternates)
                findExistingAndUpdateFQNs(
                    FullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(
                        thrownException.asClassOrInterfaceType()));

        if (syntheticType != null) {
          syntheticType.extend(new MemberType("java.lang.Throwable"));
        }
      }
    } else if (node instanceof ThrowStmt throwStmt) {
      UnsolvedClassOrInterfaceAlternates syntheticType =
          (UnsolvedClassOrInterfaceAlternates)
              findExistingAndUpdateFQNs(
                  FullyQualifiedNameGenerator.getFQNsForExpressionType(throwStmt.getExpression()));

      // The type rule dependency map will add method/type declarations to the worklist
      // way before any throw statements will be added. So, if they've already extended the
      // type, we know it is not an uncaught exception.
      if (syntheticType != null && !syntheticType.hasExtends()) {
        syntheticType.extend(new MemberType("java.lang.RuntimeException"));
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
                      FullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(
                          varDeclExpr.getElementType().asClassOrInterfaceType()));

          UnsolvedClassOrInterfaceAlternates rhs =
              (UnsolvedClassOrInterfaceAlternates)
                  findExistingAndUpdateFQNs(
                      FullyQualifiedNameGenerator.getFQNsForExpressionType(
                          varDeclExpr.getVariables().get(0).getInitializer().get()));

          types.add(lhs);
          types.add(rhs);
        } else if (resource instanceof NameExpr || resource instanceof FieldAccessExpr) {
          UnsolvedClassOrInterfaceAlternates type =
              (UnsolvedClassOrInterfaceAlternates)
                  findExistingAndUpdateFQNs(
                      FullyQualifiedNameGenerator.getFQNsForExpressionType((Expression) resource));
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
                      FullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(
                          exception.getType().asClassOrInterfaceType()));
          exceptions.add(type);
        } else if (exception.getType().isUnionType()) {
          for (ReferenceType refType : exception.getType().asUnionType().getElements()) {
            UnsolvedClassOrInterfaceAlternates type =
                (UnsolvedClassOrInterfaceAlternates)
                    findExistingAndUpdateFQNs(
                        FullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(
                            (ClassOrInterfaceType) refType));
            exceptions.add(type);
          }
        }
      }

      for (UnsolvedClassOrInterfaceAlternates exception : exceptions) {
        MemberType type = new MemberType("java.lang.Throwable");
        if (exception == null || exception.doesExtend(type)) continue;
        exception.extend(type);
      }

      for (UnsolvedClassOrInterfaceAlternates type : types) {
        if (type == null || type.doesImplement("java.lang.AutoCloseable")) continue;

        type.implement("java.lang.AutoCloseable");

        UnsolvedMethodAlternates unsolvedMethodAlternates =
            UnsolvedMethodAlternates.create(
                "close",
                new MemberType("void"),
                List.of(type),
                List.of(),
                List.of(new MemberType("java.lang.Exception")));

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
              FullyQualifiedNameGenerator.getFQNsForExpressionType(relationalExpr), false);
      UnsolvedClassOrInterfaceAlternates referenceType =
          (UnsolvedClassOrInterfaceAlternates)
              findExistingAndUpdateFQNs(
                  FullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(
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
        return new AddInformationResult();
      } catch (UnsolvedSymbolException ex) {
        // continue
      }

      List<String> simpleNames = new ArrayList<>();

      for (Expression argument : methodCall.getArguments()) {
        Set<String> fqns = FullyQualifiedNameGenerator.getFQNsForExpressionType(argument);
        String first = fqns.iterator().next();
        String simpleName = JavaParserUtil.getSimpleNameFromQualifiedName(first);

        simpleNames.add(simpleName);
      }

      Set<String> potentialFQNs = new HashSet<>();
      String methodSignature =
          methodCall.getNameAsString() + "(" + String.join(", ", simpleNames) + ")";

      Map<String, Set<String>> potentialScopeFQNs =
          FullyQualifiedNameGenerator.getFQNsForExpressionLocation(methodCall);

      if (potentialScopeFQNs.isEmpty()) {
        return new AddInformationResult();
      }

      for (Set<String> set :
          FullyQualifiedNameGenerator.getFQNsForExpressionLocation(methodCall).values()) {
        for (String potentialScopeFQN : set) {
          potentialFQNs.add(potentialScopeFQN + "#" + methodSignature);
        }
      }

      UnsolvedMethodAlternates alt =
          (UnsolvedMethodAlternates) findExistingAndUpdateFQNs(potentialFQNs);

      if (alt == null) {
        throw new RuntimeException(
            "Unresolvable method is not generated when all unsolved symbols should be.");
      }

      if (!alt.getReturnType().isUnsolved()) {
        // Already known
        return new AddInformationResult();
      }

      if (methodCall.hasScope()) {
        Set<ResolvedType> potentialTypes = new HashSet<>();
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
            return new AddInformationResult();
          }

          List<VariableDeclarator> variables;

          Node toAst = resolved.toAst().get();

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

        for (ResolvedType type : potentialTypes) {
          // Check to see if any of these contain the same method signature; if so, we can
          // update the return type of the current generated one to match it

          // Must be a reference type: if it were not, the method would be solvable, which we
          // checked
          // already
          ResolvedReferenceType refType = type.asReferenceType();

          // The type must also be a user-defined class, not a built-in Java class. This means we
          // cannot get the ResolvedMethodDeclarations from each type declaration since parameter
          // types could be unsolved
          if (refType.getTypeDeclaration().isPresent()) {
            for (ResolvedMethodDeclaration methodDecl :
                refType.getTypeDeclaration().get().getDeclaredMethods()) {
              MethodDeclaration methodDeclAst = ((MethodDeclaration) methodDecl.toAst().get());

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
                    alt.getReturnType().getUnsolvedType();
                try {
                  ResolvedType resolvedType = methodDecl.getReturnType();

                  alt.getReturnType().setSolvedType(resolvedType.describe());

                  toRemove.add(oldReturn);
                  removeSymbolFromGeneratedSymbolsMap(oldReturn);
                } catch (UnsolvedSymbolException ex) {
                  // In this case, remove the old (SomeMethodReturnType) and replace it with a
                  // "better" return
                  // type (maybe imported, so we would know a more specific FQN)
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

    return new AddInformationResult(toAdd, toRemove);
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
  private @Nullable ImportDeclaration getNameOfImport(String importedName, CompilationUnit cu) {
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
          UnsolvedClassOrInterfaceAlternates.create(fqns);

      for (UnsolvedClassOrInterfaceAlternates c : created) {
        addNewSymbolToGeneratedSymbolsMap(c);
      }

      return created.get(0);
    }

    return (UnsolvedClassOrInterfaceAlternates) existing;
  }

  /**
   * Same as {@link #findExistingAndUpdateFQNs(Set)} but creates and returns a new method if not
   * found. This only works for type FQNs.
   *
   * @param potentialFQNs
   * @return
   */

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

      if (alreadyGenerated != null) break;
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

  private void addNewSymbolToGeneratedSymbolsMap(UnsolvedSymbolAlternates<?> newSymbol) {
    for (String potentialFQN : newSymbol.getFullyQualifiedNames()) {
      if (generatedSymbols.containsKey(potentialFQN)) continue;
      generatedSymbols.put(potentialFQN, newSymbol);
    }
  }

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
  private MemberType getMemberTypeFromFQNs(Set<String> fqns) {
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
      MemberType type = getMemberTypeIfPrimitiveOrJavaLang(fqn);

      if (type != null) return type;
    }

    UnsolvedClassOrInterfaceAlternates unsolved;
    if (createNew) {
      unsolved = findExistingAndUpdateFQNsOrCreateNewType(fqns);
    } else {
      unsolved = (UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(fqns);
    }

    if (unsolved == null) {
      return null;
    } else {
      return new MemberType(unsolved);
    }
  }

  /**
   * If {@code name} (either a simple name or fully qualified) is primitive, java.lang, or in
   * another java package, then return the MemberType holding it. Else, return null.
   *
   * @param name The name of the type, either simple or fully qualified.
   */
  private @Nullable MemberType getMemberTypeIfPrimitiveOrJavaLang(String name) {
    if (JavaLangUtils.inJdkPackage(name)
        || JavaLangUtils.isJavaLangOrPrimitiveName(
            JavaParserUtil.getSimpleNameFromQualifiedName(name))) {
      return new MemberType(name);
    }
    return null;
  }
}
