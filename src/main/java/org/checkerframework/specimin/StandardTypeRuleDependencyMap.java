package org.checkerframework.specimin;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.NodeWithParameters;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithThrownExceptions;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedTypeVariable;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.DefaultConstructorDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionClassDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionInterfaceDeclaration;
import com.github.javaparser.utils.Pair;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StandardTypeRuleDependencyMap implements TypeRuleDependencyMap {

  /**
   * A map of fully-qualified names to compilation units, used to find declarations that are
   * properly attached to a compilation unit.
   */
  private final Map<String, CompilationUnit> fqnToCompilationUnits;

  public StandardTypeRuleDependencyMap(Map<String, CompilationUnit> fqnToCompilationUnits) {
    this.fqnToCompilationUnits = fqnToCompilationUnits;
  }

  /**
   * Given a node, return all relevant nodes based on its type.
   *
   * @param node The node
   * @return All relevant nodes to the input node. For example, this could be annotations, type
   *     parameters, parameters, return type, etc. for methods.
   */
  @Override
  public List<Node> getRelevantElements(Node node) {
    List<Node> elements = new ArrayList<>();

    if (node instanceof NodeWithAnnotations<?> withAnnotations) {
      for (AnnotationExpr annotation : withAnnotations.getAnnotations()) {
        if (annotation.toString().equals("@Override")) {
          // Never preserve @Override, since it causes compile errors but does not fix them.
          continue;
        } else if (annotation.toString().equals("@FunctionalInterface")) {
          // Don't preserve @FunctionalInterface until we know the method is also preserved.
          continue;
        }
        elements.add(annotation);
      }
    }
    if (node instanceof NodeWithModifiers<?> withModifiers) {
      elements.addAll(withModifiers.getModifiers());
    }
    if (node instanceof NodeWithTypeArguments<?> withTypeArguments
        && withTypeArguments.getTypeArguments().isPresent()) {
      elements.addAll(withTypeArguments.getTypeArguments().get());
    }
    if (node instanceof NodeWithTypeParameters<?> withTypeParameters) {
      elements.addAll(withTypeParameters.getTypeParameters());
    }
    // i.e., method declarations, parameters, annotation type declarations, instanceof, etc.
    if (node instanceof NodeWithType<?, ?> withType) {
      elements.add(withType.getType());
    }
    if (node instanceof NodeWithSimpleName<?> nodeWithSimpleName) {
      elements.add(nodeWithSimpleName.getName());
    }

    // Type declarations
    if (node instanceof NodeWithImplements<?> withImplements) {
      elements.addAll(withImplements.getImplementedTypes());
    }
    if (node instanceof NodeWithExtends<?> withExtends) {
      elements.addAll(withExtends.getExtendedTypes());
    }
    if (node instanceof TypeDeclaration<?> typeDeclaration) {
      elements.addAll(getAllMustImplementMethods(typeDeclaration));
    }

    // If the node is a type declaration, exit now, so we don't unintentionally
    // add extra nodes to our worklist.
    if (node instanceof TypeDeclaration) {
      return elements;
    }

    // =========================================================

    // Method declarations

    // i.e., constructor/method declarations, lambdas
    if (node instanceof NodeWithParameters<?> withParameters) {
      elements.addAll(withParameters.getParameters());
    }

    // i.e., constructor/method declarations
    if (node instanceof NodeWithThrownExceptions<?> withThrownExceptions) {
      elements.addAll(withThrownExceptions.getThrownExceptions());
    }

    // If this is a method declaration in a functional interface, preserve the
    // "@FunctionalInterface" annotation.
    if (node instanceof MethodDeclaration methodDeclaration
        && JavaParserUtil.getEnclosingClassLikeOptional(methodDeclaration)
            instanceof ClassOrInterfaceDeclaration typeDecl
        && typeDecl.isInterface()
        && typeDecl.getAnnotationByName("FunctionalInterface").isPresent()) {
      elements.add(typeDecl.getAnnotationByName("FunctionalInterface").get());
    }

    // If the node is a member declaration, exit now, so we don't unintentionally
    // add extra nodes to our worklist.
    if (node instanceof CallableDeclaration) {
      return elements;
    }

    // =========================================================

    // Statements
    // ** If a statement is included in the slice, then that means it is in one
    // of the target members. Therefore, its children are always relevant.

    // Never add variable declarators here: this prevents extra variables
    // from being included when a single field declaration has multiple variable
    // declarators.
    if (node instanceof FieldDeclaration fieldDecl) {
      for (Node child : fieldDecl.getChildNodes()) {
        if (!(child instanceof VariableDeclarator)) {
          elements.add(child);
        }
      }
      return elements;
    }

    if (node instanceof VariableDeclarator varDecl
        && varDecl.getInitializer().isPresent()
        && node.getParentNode().get() instanceof FieldDeclaration) {
      // For field declarations, don't add the initializer
      Expression initializer = varDecl.getInitializer().get();

      for (Node child : varDecl.getChildNodes()) {
        if (child.equals(initializer)) {
          continue;
        }

        elements.add(child);
      }

      return elements;
    }

    elements.addAll(node.getChildNodes());

    return elements;
  }

  @Override
  public List<Node> getRelevantElements(Object resolved) {
    List<Node> elements = new ArrayList<>();

    if (resolved instanceof ResolvedType resolvedType) {
      if (resolvedType.isArray()) {
        resolvedType = resolvedType.asArrayType().getComponentType();
      }
      if (resolvedType.isReferenceType()
          && resolvedType.asReferenceType().getTypeDeclaration().isPresent()) {
        return getRelevantElements(resolvedType.asReferenceType().getTypeDeclaration().get());
      }
    }

    if (resolved instanceof ResolvedReferenceTypeDeclaration resolvedTypeDeclaration) {
      TypeDeclaration<?> type =
          JavaParserUtil.getTypeFromQualifiedName(
              resolvedTypeDeclaration.getQualifiedName(), fqnToCompilationUnits);

      if (type == null) {
        return elements;
      }

      // Ensure outer classes are included in the slice
      TypeDeclaration<?> outerType = JavaParserUtil.getEnclosingClassLikeOptional(type);

      // Don't get all the outer classes, since it's redundant. Once this added outerType
      // is handled in the worklist, it will add the next outer class, and so on.
      if (outerType != null) {
        elements.add(outerType);
      }

      // Unfortunately, JavaParser doesn't allow us to solve annotation member value pairs,
      // so we can't tell what is used and what isn't. Preserve all annotation members for
      // now until we figure out a better solution/JavaParser adds support.
      if (resolvedTypeDeclaration.isAnnotation()) {
        elements.addAll(
            resolvedTypeDeclaration.asAnnotation().getAnnotationMembers().stream()
                .map(
                    member ->
                        type.findFirst(
                                AnnotationMemberDeclaration.class,
                                n -> n.getNameAsString().equals(member.getName()))
                            .get())
                .toList());
      }

      elements.add(type);
    }

    if (resolved instanceof ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration) {
      TypeDeclaration<?> type =
          JavaParserUtil.getTypeFromQualifiedName(
              resolvedMethodLikeDeclaration.declaringType().getQualifiedName(),
              fqnToCompilationUnits);
      if (type == null) {
        return elements;
      }

      if (resolved instanceof ResolvedMethodDeclaration resolvedMethodDeclaration) {
        elements.addAll(getAllOverriddenMethods(resolvedMethodDeclaration, type));
      }

      // Rare case: new Foo() but Foo does not contain a constructor
      if (!(resolved instanceof DefaultConstructorDeclaration)) {
        Node unattached = resolvedMethodLikeDeclaration.toAst().get();
        CallableDeclaration<?> methodLike =
            type.findFirst(CallableDeclaration.class, n -> n.equals(unattached)).get();

        elements.add(methodLike);
      }

      elements.add(type);
    }

    if (resolved instanceof ResolvedFieldDeclaration resolvedFieldDeclaration) {
      TypeDeclaration<?> type =
          JavaParserUtil.getTypeFromQualifiedName(
              resolvedFieldDeclaration.declaringType().getQualifiedName(), fqnToCompilationUnits);

      if (type == null) {
        return elements;
      }

      Node unattached = resolvedFieldDeclaration.toAst().get();
      FieldDeclaration field =
          type.findFirst(FieldDeclaration.class, n -> n.equals(unattached)).get();

      VariableDeclarator variableDeclarator =
          field.getVariables().stream()
              .filter(var -> var.getNameAsString().equals(resolvedFieldDeclaration.getName()))
              .findFirst()
              .get();

      elements.add(type);
      elements.add(field);
      elements.add(variableDeclarator);
    }

    if (resolved instanceof ResolvedEnumConstantDeclaration resolvedEnumConstantDeclaration) {
      TypeDeclaration<?> type =
          JavaParserUtil.getTypeFromQualifiedName(
              resolvedEnumConstantDeclaration.getType().describe(), fqnToCompilationUnits);

      if (type == null) {
        return elements;
      }

      Node unattached = resolvedEnumConstantDeclaration.toAst().get();
      EnumConstantDeclaration enumConstant =
          type.findFirst(EnumConstantDeclaration.class, n -> n.equals(unattached)).get();

      elements.add(type);
      elements.add(enumConstant);
    }

    return elements;
  }

  /**
   * Gets all overridden methods of the given method declaration, including those in ancestors.
   *
   * @param original The original method declaration to find overridden methods for
   * @param type The type declaration to search for overridden methods in
   * @return A list of all overridden methods
   */
  private List<MethodDeclaration> getAllOverriddenMethods(
      ResolvedMethodDeclaration original, TypeDeclaration<?> type) {
    List<MethodDeclaration> result = new ArrayList<>();

    getAllOverriddenMethodsImpl(original, type, result);
    return result;
  }

  /**
   * Helper method for {@link #getAllOverriddenMethods(ResolvedMethodDeclaration, TypeDeclaration)}.
   * This method recursively finds all overridden methods in the type declaration's ancestors.
   *
   * @param original The original method declaration to find overridden methods for
   * @param type The type declaration to search for overridden methods in
   * @param result A list to collect all overridden methods found
   */
  private void getAllOverriddenMethodsImpl(
      ResolvedMethodDeclaration original, TypeDeclaration<?> type, List<MethodDeclaration> result) {
    List<ClassOrInterfaceType> parents = new ArrayList<>();

    if (type instanceof NodeWithExtends<?> withExtends) {
      parents.addAll(withExtends.getExtendedTypes());
    }
    if (type instanceof NodeWithImplements<?> withImplements) {
      parents.addAll(withImplements.getImplementedTypes());
    }

    for (ClassOrInterfaceType parent : parents) {
      ResolvedType parentType;
      try {
        parentType = parent.resolve();
      } catch (UnsolvedSymbolException ex) {
        continue;
      }

      if (!parentType.isReferenceType()
          || parentType.asReferenceType().getTypeDeclaration().isEmpty()) {
        continue;
      }

      ResolvedReferenceTypeDeclaration decl =
          parentType.asReferenceType().getTypeDeclaration().get();

      TypeDeclaration<?> typeDecl =
          JavaParserUtil.getTypeFromQualifiedName(decl.getQualifiedName(), fqnToCompilationUnits);
      if (typeDecl == null) {
        continue;
      }

      for (MethodDeclaration method : typeDecl.getMethods()) {
        try {
          if (original
              .getSignature()
              .equals(
                  getSignatureFromResolvedMethodWithTypeVariablesMap(
                      method.resolve(), parentType.asReferenceType().getTypeParametersMap()))) {
            result.add(method);
          }
        } catch (UnsolvedSymbolException ex) {
          // At least one parameter type may not be solvable. In this case, try comparing
          // simple
          // names.
          if (areAstAndResolvedMethodLikelyEqual(original, method)) {
            result.add(method);
          }
        }
      }

      getAllOverriddenMethodsImpl(original, typeDecl, result);
    }
  }

  /**
   * Given a type declaration, return all must implement methods to the result list. Must implement
   * methods are methods that are abstract in JDK superclasses or non-default methods in JDK
   * interfaces.
   *
   * @param typeDecl The type declaration to which the parents belong
   */
  private List<MethodDeclaration> getAllMustImplementMethods(TypeDeclaration<?> typeDecl) {
    List<MethodDeclaration> methods = new ArrayList<>();
    Set<MethodDeclaration> alreadyImplemented = new HashSet<>();

    List<ClassOrInterfaceType> parents = new ArrayList<>();

    if (typeDecl instanceof NodeWithExtends<?> withExtends) {
      parents.addAll(withExtends.getExtendedTypes());
    }
    if (typeDecl instanceof NodeWithImplements<?> withImplements) {
      parents.addAll(withImplements.getImplementedTypes());
    }

    getAllMustImplementMethodsImpl(parents, typeDecl, typeDecl, methods, alreadyImplemented);

    methods.removeAll(alreadyImplemented);
    return methods;
  }

  /**
   * Helper method for getAllMustImplementMethods. This method recursively finds all must implement
   * methods in the type declaration's parents and ancestors.
   *
   * @param parents The parents of the type declaration, i.e., the extended/implemented types
   * @param originalTypeDecl The original type declaration
   * @param typeDecl The current type declaration being processed
   * @param result The result list
   * @param alreadyImplemented A set of methods that are already implemented in a superclass or
   *     interface
   */
  private void getAllMustImplementMethodsImpl(
      List<ClassOrInterfaceType> parents,
      TypeDeclaration<?> originalTypeDecl,
      TypeDeclaration<?> typeDecl,
      List<MethodDeclaration> result,
      Set<MethodDeclaration> alreadyImplemented) {
    for (ClassOrInterfaceType type : parents) {
      try {
        ResolvedType resolved = type.resolve();

        if (!resolved.isReferenceType()) {
          continue;
        }

        boolean isInterface = false;
        ResolvedReferenceTypeDeclaration resolvedTypeDecl =
            resolved.asReferenceType().getTypeDeclaration().orElse(null);

        Set<ResolvedMethodDeclaration> methods;

        if (resolvedTypeDecl instanceof ReflectionClassDeclaration reflectionClassDeclaration) {
          methods = reflectionClassDeclaration.getDeclaredMethods();
        } else if (resolvedTypeDecl
            instanceof ReflectionInterfaceDeclaration reflectionInterfaceDeclaration) {
          isInterface = true;
          methods = reflectionInterfaceDeclaration.getDeclaredMethods();
        } else {
          continue;
        }

        for (ResolvedMethodDeclaration resolvedMethodDecl : methods) {
          for (MethodDeclaration methodInOriginal :
              originalTypeDecl.findAll(MethodDeclaration.class)) {
            ResolvedMethodDeclaration methodInOriginalResolved = methodInOriginal.resolve();

            // TODO: use a better type parameter map; this one only works for direct parents
            // For example, if current is Foo<String> and Foo<T> extends List<T>, and List is
            // defined as List<E> then we need to get E --> String, not E --> T.
            if (methodInOriginalResolved
                .getSignature()
                .equals(
                    getSignatureFromResolvedMethodWithTypeVariablesMap(
                        resolvedMethodDecl, resolved.asReferenceType().getTypeParametersMap()))) {
              if ((!isInterface && !resolvedMethodDecl.isAbstract())
                  || (isInterface && resolvedMethodDecl.isDefaultMethod())) {
                alreadyImplemented.add(methodInOriginal);
                break;
              }

              result.add(methodInOriginal);
              break;
            }
          }
        }
      } catch (UnsolvedSymbolException ex) {
        // continue
      }
    }

    for (TypeDeclaration<?> ancestor :
        JavaParserUtil.getAllSolvableAncestors(typeDecl, fqnToCompilationUnits)) {
      if (ancestor instanceof NodeWithExtends<?> withExtends) {
        getAllMustImplementMethodsImpl(
            withExtends.getExtendedTypes(), originalTypeDecl, ancestor, result, alreadyImplemented);
      }
      if (ancestor instanceof NodeWithImplements<?> withImplements) {
        getAllMustImplementMethodsImpl(
            withImplements.getImplementedTypes(),
            originalTypeDecl,
            ancestor,
            result,
            alreadyImplemented);
      }
    }
  }

  /**
   * Given a resolved method declaration and its declaring type's type variables map, return the
   * method's signature with the type variables replaced by their resolved types.
   *
   * <p>For example, if the method is part of Foo<T> and the type variables map is T --> String,
   * then any parameters that match T will be replaced with String in the signature.
   *
   * @param method The resolved method declaration in the generic class
   * @param typeVariablesMap The type variables map, which maps type variable declarations to their
   *     resolved types
   * @return The method's signature with the type variables replaced by their resolved types
   */
  private String getSignatureFromResolvedMethodWithTypeVariablesMap(
      ResolvedMethodDeclaration method,
      List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> typeVariablesMap) {
    StringBuilder signature = new StringBuilder(method.getName() + "(");

    for (int i = 0; i < method.getNumberOfParams(); i++) {
      ResolvedParameterDeclaration param = method.getParam(i);

      if (param.getType().isTypeVariable()) {
        ResolvedTypeVariable typeVariable = param.getType().asTypeVariable();
        for (Pair<ResolvedTypeParameterDeclaration, ResolvedType> pair : typeVariablesMap) {
          if (pair.a.equals(typeVariable.asTypeParameter())) {
            signature.append(pair.b.describe());
            break;
          }
        }
      } else {
        signature.append(param.getType().describe());
      }
    }

    signature.append(")");

    return signature.toString();
  }

  /**
   * Checks to see if a resolved method declaration and a method declaration AST node are likely to
   * be the same method, based on their names and the simple names of their parameters. Use this
   * method only when {@code ast} is not resolvable and you can't compare with qualified parameter
   * types.
   *
   * @param resolved The resolved method declaration
   * @param ast The method declaration AST node
   * @return true if the method and AST node are likely to be the same method, false otherwise
   */
  private boolean areAstAndResolvedMethodLikelyEqual(
      ResolvedMethodDeclaration resolved, MethodDeclaration ast) {
    if (!ast.getNameAsString().equals(resolved.getName())) {
      return false;
    }

    if (ast.getParameters().size() != resolved.getNumberOfParams()) {
      return false;
    }

    for (int i = 0; i < ast.getParameters().size(); i++) {
      String resolvedParamType;
      try {
        resolvedParamType = resolved.getParam(i).getType().describe();
      } catch (UnsolvedSymbolException ex) {
        // See if the AST version exists, and use that simple name
        if (resolved.toAst().orElse(null) instanceof MethodDeclaration methodDecl) {
          resolvedParamType = methodDecl.getParameter(i).getType().toString();
        } else {
          // If we cannot compare, we'll return false
          return false;
        }
      }

      if (!JavaParserUtil.getSimpleNameFromQualifiedName(resolvedParamType)
          .equals(
              JavaParserUtil.getSimpleNameFromQualifiedName(
                  ast.getParameter(i).getType().toString()))) {
        return false;
      }
    }

    return true;
  }
}
