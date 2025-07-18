package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    List<UnsolvedSymbolAlternates<?>> generated = inferContextButDoNotAddInformation(node);

    for (UnsolvedSymbolAlternates<?> alternate : generated) {
      addInformation(node, alternate);
    }

    return generated;
  }

  /**
   * Equivalent to {@code inferContext} but does not call {@code addInformation}.
   *
   * @param node The unresolvable node
   * @return A list of UnsolvedSymbolAlternates generated/found from the input
   */
  private List<UnsolvedSymbolAlternates<?>> inferContextButDoNotAddInformation(Node node) {
    List<UnsolvedSymbolAlternates<?>> generated = new ArrayList<>();

    inferContextImpl(node, generated);

    for (UnsolvedSymbolAlternates<?> alternate : generated) {
      for (String fqn : alternate.getFullyQualifiedNames()) {
        generatedSymbols.put(fqn, alternate);
      }
    }

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

    // Since this method is called recursively, we may encounter cases where a node is resolvable.
    // In this case,
    // return early since we do not want to generate a synthetic type for it.
    // This could occur when a MethodCallExpr is not resolvable, but its arguments could be.

    boolean resolvable = false;
    if (node instanceof Resolvable<?>) {
      try {
        ((Resolvable<?>) node).resolve();
        resolvable = true;

        // MethodReferenceExprs are special: we also need to generate its functional interface for
        // later use
        if (!(node instanceof MethodReferenceExpr)) {
          return;
        }
      } catch (UnsolvedSymbolException ex) {

      }
    }

    // Types
    if (node instanceof ClassOrInterfaceType) {
      ClassOrInterfaceType asType = (ClassOrInterfaceType) node;

      Set<String> potentialFQNs =
          FullyQualifiedNameGenerator.getFQNsFromClassOrInterfaceType(asType);

      UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(potentialFQNs);

      if (generated == null) {
        UnsolvedClassOrInterfaceAlternates newlyGenerated =
            UnsolvedClassOrInterfaceAlternates.create(potentialFQNs);

        if (asType.getTypeArguments().isPresent()) {
          newlyGenerated.setNumberOfTypeVariables(asType.getTypeArguments().get().size());
        }

        generated = newlyGenerated;
      }

      result.add(generated);
    } else if (node instanceof AnnotationExpr) {
      AnnotationExpr asAnno = (AnnotationExpr) node;

      Set<String> potentialFQNs = FullyQualifiedNameGenerator.getFQNsFromAnnotation(asAnno);

      UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(potentialFQNs);

      if (generated == null) {
        UnsolvedClassOrInterfaceAlternates newAnno =
            UnsolvedClassOrInterfaceAlternates.create(potentialFQNs);
        newAnno.setIsAnAnnotationToTrue();
        generated = newAnno;
      }

      result.add(generated);
    }
    // Fields (although types are handled first conditional in FieldAccessExpr)
    else if (node instanceof FieldAccessExpr) {
      // When we have a FieldAccessExpr like a.b.c, the scope a.b is also a FieldAccessExpr
      // We need to handle the case where the scope could be a class, like org.example.MyClass,
      // because resolving the scope of a static field like org.example.MyClass.a would return
      // another FieldAccessExpr, not a ClassOrInterfaceType
      if (node.getParentNode().isPresent()
          && node.getParentNode().get() instanceof FieldAccessExpr
          && JavaParserUtil.isAClassPath(node.toString())) {
        Set<String> potentialFQNs =
            FullyQualifiedNameGenerator.getFQNsForExpressionType((FieldAccessExpr) node);

        UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(potentialFQNs);

        if (generated == null) {
          generated = UnsolvedClassOrInterfaceAlternates.create(potentialFQNs);
        }

        result.add(generated);
        return;
      }

      FieldAccessExpr asField = (FieldAccessExpr) node;

      Expression scope = asField.getScope();

      // Generate everything in the scopes before
      // Call inferContextButDoNotAddInformation instead of inferContextImpl because we want
      // generated symbols to be added
      // to the map
      result.addAll(inferContextButDoNotAddInformation(scope));

      Map<String, Set<String>> potentialScopeFQNs =
          FullyQualifiedNameGenerator.getFQNsForExpressionLocation(scope);
      Set<String> potentialFQNs = new HashSet<>();

      for (Set<String> set : potentialScopeFQNs.values()) {
        for (String potentialScopeFQN : set) {
          potentialFQNs.add(potentialScopeFQN + "#" + asField.getNameAsString());
        }
      }

      UnsolvedSymbolAlternates<?> alreadyGenerated = findExistingAndUpdateFQNs(potentialFQNs);
      UnsolvedFieldAlternates unsolvedField;

      if (alreadyGenerated instanceof UnsolvedFieldAlternates) {
        unsolvedField = (UnsolvedFieldAlternates) alreadyGenerated;
      } else {
        Set<String> potentialTypeFQNs =
            FullyQualifiedNameGenerator.getFQNsForExpressionType(asField);

        // Since we called inferContextButDoNotAddInformation(scope), the field's parents are
        // created
        List<UnsolvedClassOrInterfaceAlternates> potentialParents = new ArrayList<>();
        for (Set<String> set : potentialScopeFQNs.values()) {
          potentialParents.add((UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(set));
        }

        MemberType type = getMemberTypeFromFQNs(potentialTypeFQNs);

        boolean isStatic = JavaParserUtil.isAClassPath(scope.toString());

        unsolvedField =
            UnsolvedFieldAlternates.create(
                asField.getNameAsString(), type, potentialParents, isStatic, false);
      }

      result.add(unsolvedField);
    } else if (node instanceof NameExpr) {
      NameExpr nameExpr = (NameExpr) node;

      // 1) static import
      CompilationUnit cu = node.findCompilationUnit().get();

      ImportDeclaration staticImport = null;

      for (ImportDeclaration importDecl : cu.getImports()) {
        if (!importDecl.isStatic()) continue;

        if (importDecl.getNameAsString().endsWith("." + nameExpr.getNameAsString())) {
          staticImport = importDecl;
        }
      }

      if (staticImport != null) {
        String staticImportFqn =
            staticImport.getName().getQualifier().get()
                + "#"
                + staticImport.getName().getIdentifier();

        String encapsulatingClass = staticImport.getName().getQualifier().get().toString();

        UnsolvedSymbolAlternates<?> generatedField =
            findExistingAndUpdateFQNs(Set.of(staticImportFqn));

        if (!(generatedField instanceof UnsolvedFieldAlternates)) {
          // Generate/find the class that will hold the field
          UnsolvedSymbolAlternates<?> generatedClass =
              findExistingAndUpdateFQNs(Set.of(encapsulatingClass));

          if (generatedClass == null) {
            generatedClass = UnsolvedClassOrInterfaceAlternates.create(Set.of(encapsulatingClass));
          }

          // Generate the synthetic type
          String typeFQN =
              FullyQualifiedNameGenerator.getFQNOfStaticallyImportedFieldType(encapsulatingClass);

          UnsolvedSymbolAlternates<?> generatedType = findExistingAndUpdateFQNs(Set.of(typeFQN));

          if (generatedType == null) {
            generatedType = UnsolvedClassOrInterfaceAlternates.create(Set.of(typeFQN));
          }

          generatedField =
              UnsolvedFieldAlternates.create(
                  staticImport.getName().getIdentifier(),
                  MemberType.of(generatedType),
                  Set.of((UnsolvedClassOrInterfaceAlternates) generatedClass),
                  true,
                  true);

          result.add(generatedField);

          return;
        }
      }

      // 2) super class field
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
          UnsolvedSymbolAlternates<?> generatedClass = findExistingAndUpdateFQNs(fqns);

          if (generatedClass == null) {
            generatedClass = UnsolvedClassOrInterfaceAlternates.create(fqns);
          }

          generatedClasses.add((UnsolvedClassOrInterfaceAlternates) generatedClass);
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
          UnsolvedSymbolAlternates<?> generatedType = findExistingAndUpdateFQNs(typeFQNs);

          if (generatedType == null) {
            generatedType = UnsolvedClassOrInterfaceAlternates.create(typeFQNs);
          }

          type = MemberType.of(generatedType);
          result.add(generatedType);
        }

        generatedField =
            UnsolvedFieldAlternates.create(
                nameExpr.getNameAsString(), type, generatedClasses, false, false);

        result.add(generatedField);
      }
    }
    // Methods
    else if (node instanceof MethodCallExpr) {
      MethodCallExpr methodCall = (MethodCallExpr) node;

      // A collection of sets of fqns. Each set represents potentially a different class/interface.
      Collection<Set<String>> potentialScopeFQNs = null;
      boolean isStaticImport = false;

      String erasedMethodName = JavaParserUtil.erase(methodCall.getNameAsString());

      // Static import
      if (!methodCall.hasScope()) {
        CompilationUnit cu = node.findCompilationUnit().get();

        ImportDeclaration staticImport = null;

        for (ImportDeclaration importDecl : cu.getImports()) {
          if (!importDecl.isStatic()) continue;

          if (importDecl.getNameAsString().endsWith("." + erasedMethodName)) {
            staticImport = importDecl;
          }
        }

        if (staticImport != null) {
          potentialScopeFQNs =
              Set.of(Set.of(staticImport.getName().getQualifier().get().toString()));
          isStaticImport = true;
        }
      } else {
        result.addAll(inferContextButDoNotAddInformation(methodCall.getScope().get()));
      }

      if (potentialScopeFQNs == null) {
        potentialScopeFQNs =
            FullyQualifiedNameGenerator.getFQNsForExpressionLocation(methodCall.getScope().get())
                .values();
      }

      Map<Expression, String> argumentToSimpleName = new HashMap<>();
      Map<String, Set<String>> simpleNameToParameterPotentialFQNs = new HashMap<>();

      for (Expression argument : methodCall.getArguments()) {
        Set<String> fqns = FullyQualifiedNameGenerator.getFQNsForExpressionType(argument);
        String first = fqns.iterator().next();
        String simpleName = JavaParserUtil.getSimpleNameFromQualifiedName(first);
        simpleNameToParameterPotentialFQNs.put(simpleName, fqns);
        argumentToSimpleName.put(argument, simpleName);
      }

      Set<String> potentialFQNs = new HashSet<>();

      for (Set<String> set : potentialScopeFQNs) {
        for (String potentialScopeFQN : set) {
          potentialFQNs.add(
              potentialScopeFQN
                  + "#"
                  + erasedMethodName
                  + "("
                  + String.join(",", simpleNameToParameterPotentialFQNs.keySet())
                  + ")");
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
          potentialParents.add((UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(set));
        }

        MemberType returnType = getMemberTypeFromFQNs(potentialReturnTypeFQNs);

        List<MemberType> parameters = new ArrayList<>();

        for (Expression argument : methodCall.getArguments()) {
          inferContextButDoNotAddInformation(argument);
          parameters.add(
              getMemberTypeFromFQNs(
                  simpleNameToParameterPotentialFQNs.get(argumentToSimpleName.get(argument))));
        }

        generatedMethod =
            UnsolvedMethodAlternates.create(
                erasedMethodName, returnType, potentialParents, parameters);

        if (methodCall.getTypeArguments().isPresent()) {
          generatedMethod.setNumberOfTypeVariables(methodCall.getTypeArguments().get().size());
        }
      }

      if (isStaticImport) {
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
        scope =
            (UnsolvedClassOrInterfaceAlternates)
                inferContextButDoNotAddInformation(constructor.getType()).get(0);

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

          scope =
              (UnsolvedClassOrInterfaceAlternates)
                  inferContextButDoNotAddInformation(superClass).get(0);

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
                + String.join(",", simpleNameToParameterPotentialFQNs.keySet())
                + ")");
      }

      UnsolvedSymbolAlternates<?> generated = findExistingAndUpdateFQNs(potentialFQNs);
      UnsolvedMethodAlternates generatedMethod;

      if (generated instanceof UnsolvedMethodAlternates) {
        generatedMethod = (UnsolvedMethodAlternates) generated;
      } else {
        List<MemberType> parameters = new ArrayList<>();

        for (Expression argument : arguments) {
          inferContextButDoNotAddInformation(argument);
          parameters.add(
              getMemberTypeFromFQNs(
                  simpleNameToParameterPotentialFQNs.get(argumentToSimpleName.get(argument))));
        }

        generatedMethod =
            UnsolvedMethodAlternates.create(
                constructorName, MemberType.of(""), List.of(scope), parameters);

        generatedMethod.setNumberOfTypeVariables(numberOfTypeParams);
      }

      result.add(generatedMethod);
    }
    // Method references
    // In practice, MethodReferenceExpr may never resolve. JavaParser resolve on
    // MethodReferenceExprs only
    // resolves if the LHS is also resolvable, which is often not the case in this method.
    // Instead, we'll need to rely on JavaTypeCorrect to give us the correct functional interface.
    else if (node instanceof MethodReferenceExpr && !resolvable) {
      MethodReferenceExpr methodRef = (MethodReferenceExpr) node;

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
                MemberType.of(isConstructor ? "" : "void"),
                scope,
                parameters);

        if (methodRef.getTypeArguments().isPresent()) {
          generatedMethod.setNumberOfTypeVariables(methodRef.getTypeArguments().get().size());
        }
      }

      result.add(generatedMethod);
    }
    // A lambda expr is not of type Resolvable<?>, but it could be passed into this method
    // when an argument is a lambda.
    else if (node instanceof LambdaExpr || node instanceof MethodReferenceExpr) {
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
          UnsolvedClassOrInterfaceAlternates.create(erasedPotentialFQNs);
      functionalInterface.setNumberOfTypeVariables(arity + (isVoid ? 1 : 0));
      result.add(functionalInterface);

      String[] paramArray =
          functionalInterface.getTypeVariablesAsStringWithoutBrackets().split(", ");
      List<MemberType> params = new ArrayList<>();

      // remove the last element of params, because that's the return type, not a parameter
      for (int i = 0; i < params.size() - (isVoid ? 1 : 0); i++) {
        params.add(MemberType.of(paramArray[i]));
      }

      String returnType = isVoid ? "void" : "T" + arity;
      UnsolvedMethodAlternates apply =
          UnsolvedMethodAlternates.create(
              "apply", MemberType.of(returnType), List.of(functionalInterface), params);

      result.add(apply);
    }
  }

  /**
   * Finds the existing unsolved symbol based on a set of potential FQNs. If none is found, this
   * method returns null. The generatedSymbols map is also modified if the intersection of
   * potentialFQNs and the existing set results in a smaller set of potential FQNs.
   *
   * @param potentialFQNs The set of potential fully-qualified names in the current context.
   * @return The existing symbol, or null if one does not exist yet.
   */
  private UnsolvedSymbolAlternates<?> findExistingAndUpdateFQNs(Set<String> potentialFQNs) {
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
              generatedSymbols.get(potentialFQN.substring(potentialFQN.lastIndexOf('#')));

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
   * Call this method each time an unsolved symbol is found. Pass in the synthetic symbol from the
   * slice and this method will update it based on additional context found in the node. This method
   * does NOT create a new synthetic definition.
   *
   * @param node The node to gain context from
   * @param synthetic The synthetic definition
   */
  private void addInformation(Node node, UnsolvedSymbolAlternates<?> synthetic) {}

  /**
   * Gets the {@code MemberType} from a set of FQNs. If one of the FQNs represents a primitive or
   * built-in java class, then it returns that type. If not, then this method will find an existing
   * generated type and return it; note that this class does not generate new types.
   *
   * @param fqns The set of fully-qualified names
   * @return The member type
   */
  private MemberType getMemberTypeFromFQNs(Set<String> fqns) {
    for (String fqn : fqns) {
      MemberType type = getMemberTypeIfPrimitiveOrJavaLang(fqn);

      if (type != null) return type;
    }

    return MemberType.of((UnsolvedClassOrInterfaceAlternates) findExistingAndUpdateFQNs(fqns));
  }

  /**
   * If {@code name} (either a simple name or fully qualified) is primitive, java.lang, or in
   * another java package, then return the MemberType holding it. Else, return null.
   *
   * @param name The name of the type, either simple or fully qualified.
   */
  private MemberType getMemberTypeIfPrimitiveOrJavaLang(String name) {
    if (JavaLangUtils.inJdkPackage(name)
        || JavaLangUtils.isJavaLangOrPrimitiveName(
            JavaParserUtil.getSimpleNameFromQualifiedName(name))) {
      return MemberType.of(name);
    }
    return null;
  }
}
