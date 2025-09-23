package org.checkerframework.specimin;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
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
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.DefaultConstructorDeclaration;
import com.github.javaparser.utils.Pair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** The standard type rule dependency map */
public class StandardTypeRuleDependencyMap implements TypeRuleDependencyMap {

  /**
   * A map of fully-qualified names to compilation units, used to find declarations that are
   * properly attached to a compilation unit.
   */
  private final Map<String, CompilationUnit> fqnToCompilationUnits;

  /**
   * Creates a new StandardTypeRuleDependencyMap to be passed into Slicer.
   *
   * @param fqnToCompilationUnits The map of type FQNs to their compilation units.
   */
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
      elements.addAll(
          JavaParserUtil.getDeclarationsForAllMustImplementMethods(
              typeDeclaration, fqnToCompilationUnits));
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

    if (node instanceof ConstructorDeclaration constructor) {
      TypeDeclaration<?> type = JavaParserUtil.getEnclosingClassLike(node);

      if (type instanceof NodeWithExtends<?> withExtends
          && withExtends.getExtendedTypes().size() == 1) {
        // First, check if the superclass is resolvable. If it is, then check to see if there is a
        // default constructor. If not, then we must preserve the explicit constructor invocation
        // statement in all constructors that are being preserved.

        ClassOrInterfaceType parentType = withExtends.getExtendedTypes().get(0);
        try {
          ResolvedType parentResolvedType = parentType.resolve();

          if (parentResolvedType.isReferenceType()
              && parentResolvedType.asReferenceType().getTypeDeclaration().isPresent()) {
            ResolvedReferenceTypeDeclaration parentDecl =
                parentResolvedType.asReferenceType().getTypeDeclaration().get();

            boolean hasDefaultConstructor = parentDecl.getConstructors().size() == 0;

            for (ResolvedConstructorDeclaration resolvedConstructor :
                parentDecl.getConstructors()) {
              if (resolvedConstructor.getNumberOfParams() == 0) {
                hasDefaultConstructor = true;
                break;
              }
            }

            if (!hasDefaultConstructor) {
              elements.add(constructor.getBody());

              // No default constructor = first statement must be super()/this()
              Statement firstStatement =
                  constructor.getBody().getStatements().stream()
                      .filter(s -> s.isExplicitConstructorInvocationStmt())
                      .findFirst()
                      .get();

              elements.add(firstStatement);
            }
          }
        } catch (UnsolvedSymbolException ex) {
          // Always preserve super/this if the parent type is not resolvable, since we don't
          // know if there is a default constructor. See UnsolvedSuperConstructor2Test for an
          // example of why this is necessary.

          Statement firstStatement =
              constructor.getBody().getStatements().stream()
                  .filter(s -> s.isExplicitConstructorInvocationStmt())
                  .findFirst()
                  .orElse(null);

          if (firstStatement != null) {
            elements.add(constructor.getBody());
            elements.add(firstStatement);
          }
        }
      }
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

    // If a constructor's block statement is included, we shouldn't add all its child statements
    // because we only want
    // to preserve super(). If this is the target, TargetMemberFinderVisitor will have added the
    // statements already.
    if (node instanceof BlockStmt
        && node.getParentNode().orElse(null) instanceof ConstructorDeclaration) {
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

    if (node instanceof ObjectCreationExpr objectCreationExpr
        && objectCreationExpr.getAnonymousClassBody().isPresent()) {
      List<BodyDeclaration<?>> anonymousClassBody =
          objectCreationExpr.getAnonymousClassBody().get();
      // Must preserve everything in the anonymous class body
      for (BodyDeclaration<?> bodyDeclaration : anonymousClassBody) {
        elements.add(bodyDeclaration);

        // Need to call getChildNodes() since adding a method/field/constructor declaration
        // will not add its content. Still, we must be careful about adding @Override.
        for (Node child : bodyDeclaration.getChildNodes()) {
          if (child instanceof AnnotationExpr annotation
              && annotation.toString().equals("@Override")) {
            continue;
          }

          elements.add(child);

          // By default, variable declarator initializers are not preserved
          if (child instanceof VariableDeclarator varDecl && varDecl.getInitializer().isPresent()) {
            elements.add(varDecl.getInitializer().get());
          }
        }
      }
    }

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
      TypeDeclaration<?> type;
      boolean isAnonymousClass = false;
      List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> typeParametersMapForAnonClass =
          null;
      // Check to see if this method is in an anonymous class
      if (resolvedMethodLikeDeclaration.toAst().isPresent()
          && resolvedMethodLikeDeclaration.toAst().get().getParentNode().get()
              instanceof ObjectCreationExpr objCreationExpr) {
        try {
          // Try to get the parent class of the anonymous class
          ResolvedType resolvedAnonParent = objCreationExpr.getType().resolve();
          type =
              JavaParserUtil.getTypeFromQualifiedName(
                  resolvedAnonParent.describe(), fqnToCompilationUnits);
          typeParametersMapForAnonClass =
              resolvedAnonParent.asReferenceType().getTypeParametersMap();
          isAnonymousClass = true;
        } catch (UnsolvedSymbolException ex) {
          // Handle in UnsolvedSymbolGenerator
          return elements;
        }
      } else {
        type =
            JavaParserUtil.getTypeFromQualifiedName(
                resolvedMethodLikeDeclaration.declaringType().getQualifiedName(),
                fqnToCompilationUnits);
      }

      if (type == null) {
        return elements;
      }

      if (resolved instanceof ResolvedMethodDeclaration resolvedMethodDeclaration) {
        if (isAnonymousClass && typeParametersMapForAnonClass != null) {
          // The current type is already a parent class, so we need to add those too
          List<MethodDeclaration> methods = new ArrayList<>();
          addOverriddenMethodsToList(
              type, resolvedMethodDeclaration, typeParametersMapForAnonClass, methods);
          elements.addAll(methods);
        }

        elements.addAll(getAllOverriddenMethods(resolvedMethodDeclaration, type));
      }

      // Case: new Foo() but Foo does not contain a constructor
      // Anonymous class methods do not need to be re-added
      if (!(resolved instanceof DefaultConstructorDeclaration)
          && !isAnonymousClass
          && resolvedMethodLikeDeclaration.toAst().isPresent()) {
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

    if (resolved instanceof ResolvedEnumConstantDeclaration resolvedEnumConstantDeclaration
        && resolvedEnumConstantDeclaration.toAst().isPresent()) {
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

      // Most of the time, this method will return one constructor. However, there may be cases
      // where we include multiple constructors, but this is because we simply don't know which
      // one to use.
      List<? extends CallableDeclaration<?>> constructors =
          JavaParserUtil.tryResolveNodeWithUnresolvableArguments(
              enumConstant, fqnToCompilationUnits);
      elements.addAll(constructors);
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
    List<ClassOrInterfaceType> parents = JavaParserUtil.getDirectSuperTypes(type);

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

      addOverriddenMethodsToList(
          typeDecl, original, parentType.asReferenceType().getTypeParametersMap(), result);

      getAllOverriddenMethodsImpl(original, typeDecl, result);
    }
  }

  /**
   * Helper method to add methods of matching signature to {@code original} from {@code typeDecl} to
   * {@code result}.
   *
   * @param typeDecl The type declaration to search for overridden methods in
   * @param original The original method declaration to find overridden methods for
   * @param typeParametersMap The type parameters map
   * @param result A list to collect all overridden methods found
   */
  private void addOverriddenMethodsToList(
      TypeDeclaration<?> typeDecl,
      ResolvedMethodDeclaration original,
      List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> typeParametersMap,
      List<MethodDeclaration> result) {
    for (MethodDeclaration method : typeDecl.getMethods()) {
      try {
        if (original
            .getSignature()
            .equals(
                JavaParserUtil.getSignatureFromResolvedMethodWithTypeVariablesMap(
                    method.resolve(), typeParametersMap))) {
          result.add(method);
        }
      } catch (UnsolvedSymbolException ex) {
        // At least one parameter type may not be solvable. In this case, try comparing
        // simple names.
        if (areAstAndResolvedMethodLikelyEqual(original, method)) {
          result.add(method);
        }
      }
    }
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
