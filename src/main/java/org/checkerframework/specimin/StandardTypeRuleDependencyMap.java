package org.checkerframework.specimin;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.nodeTypes.NodeWithBlockStmt;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.nodeTypes.NodeWithCondition;
import com.github.javaparser.ast.nodeTypes.NodeWithExpression;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import com.github.javaparser.ast.nodeTypes.NodeWithOptionalScope;
import com.github.javaparser.ast.nodeTypes.NodeWithParameters;
import com.github.javaparser.ast.nodeTypes.NodeWithScope;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithStatements;
import com.github.javaparser.ast.nodeTypes.NodeWithThrownExceptions;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    if (node instanceof NodeWithAnnotations) {
      NodeWithAnnotations<?> withAnnotations = (NodeWithAnnotations<?>) node;

      elements.addAll(withAnnotations.getAnnotations());
    }
    if (node instanceof NodeWithModifiers) {
      NodeWithModifiers<?> withModifiers = (NodeWithModifiers<?>) node;

      elements.addAll(withModifiers.getModifiers());
    }
    if (node instanceof NodeWithTypeArguments) {
      NodeWithTypeArguments<?> withTypeArguments = (NodeWithTypeArguments<?>) node;

      if (withTypeArguments.getTypeArguments().isPresent()) {
        elements.addAll(withTypeArguments.getTypeArguments().get());
      }
    }
    if (node instanceof NodeWithTypeParameters) {
      NodeWithTypeParameters<?> withTypeParameters = (NodeWithTypeParameters<?>) node;

      elements.addAll(withTypeParameters.getTypeParameters());
    }
    // i.e., method declarations, parameters, annotation type declarations, instanceof, etc.
    if (node instanceof NodeWithType) {
      NodeWithType<?, ?> withType = (NodeWithType<?, ?>) node;

      elements.add(withType.getType());
    }
    if (node instanceof NodeWithSimpleName) {
      NodeWithSimpleName<?> nodeWithSimpleName = (NodeWithSimpleName<?>) node;

      elements.add(nodeWithSimpleName.getName());
    }

    // Type declarations
    if (node instanceof NodeWithImplements) {
      NodeWithImplements<?> withImplements = (NodeWithImplements<?>) node;

      elements.addAll(withImplements.getImplementedTypes());
    }
    if (node instanceof NodeWithExtends) {
      NodeWithExtends<?> withExtends = (NodeWithExtends<?>) node;

      elements.addAll(withExtends.getExtendedTypes());
    }

    // If the node is a type declaration, exit now, so we don't unintentionally
    // add extra nodes to our worklist.
    if (node instanceof TypeDeclaration) {
      return elements;
    }

    // =========================================================

    // Method declarations

    // i.e., constructor/method declarations, lambdas
    if (node instanceof NodeWithParameters) {
      NodeWithParameters<?> withParameters = (NodeWithParameters<?>) node;

      elements.addAll(withParameters.getParameters());
    }

    // i.e., constructor/method declarations
    if (node instanceof NodeWithThrownExceptions) {
      NodeWithThrownExceptions<?> withThrownExceptions = (NodeWithThrownExceptions<?>) node;

      elements.addAll(withThrownExceptions.getThrownExceptions());

      return elements;
    }

    // If the node is a callable declaration, exit now, so we don't unintentionally
    // add extra nodes to our worklist.
    if (node instanceof CallableDeclaration) {
      return elements;
    }

    // =========================================================

    // Method calls

    // i.e., call.func(...), new Class(...)
    if (node instanceof NodeWithOptionalScope) {
      NodeWithOptionalScope<?> withOptionalScope = (NodeWithOptionalScope<?>) node;

      if (withOptionalScope.getScope().isPresent()) {
        elements.add(withOptionalScope.getScope().get());
      }
    }

    // i.e., enum constant declaration, super(...), call.func(...), new Class(...)
    if (node instanceof NodeWithArguments) {
      NodeWithArguments<?> withArguments = (NodeWithArguments<?>) node;

      elements.addAll(withArguments.getArguments());

      // If the node is a method call, exit now, so we don't unintentionally
      // add extra nodes to our worklist.
      return elements;
    }

    // =========================================================

    // Statements
    // ** If a statement is included in the slice, then that means it is in one
    // of the target members. Therefore, its children are always relevant.

    if (node instanceof NodeWithBlockStmt) {
      NodeWithBlockStmt<?> withBlockStmt = (NodeWithBlockStmt<?>) node;

      elements.add(withBlockStmt.getBody());
    }
    // i.e., do, foreach, for, while
    if (node instanceof NodeWithBody) {
      NodeWithBody<?> withBody = (NodeWithBody<?>) node;

      elements.add(withBody.getBody());
    }
    // i.e., ?:, do, if, while
    if (node instanceof NodeWithCondition) {
      NodeWithCondition<?> withCondition = (NodeWithCondition<?>) node;

      elements.add(withCondition.getCondition());
    }
    // i.e., casts, instanceof, synchronized, throw, unary expressions, lambda body
    if (node instanceof NodeWithExpression) {
      NodeWithExpression<?> withExpression = (NodeWithExpression<?>) node;

      elements.add(withExpression.getExpression());
    }
    if (node instanceof SingleMemberAnnotationExpr) {
      SingleMemberAnnotationExpr anno = (SingleMemberAnnotationExpr) node;

      elements.add(anno.getMemberValue());
    }
    if (node instanceof NormalAnnotationExpr) {
      NormalAnnotationExpr anno = (NormalAnnotationExpr) node;

      elements.addAll(anno.getPairs());
    }
    if (node instanceof NodeWithScope) {
      NodeWithScope<?> withScope = (NodeWithScope<?>) node;

      elements.add(withScope.getScope());
    }
    // i.e., switch entry / lambda body
    if (node instanceof NodeWithStatements) {
      NodeWithStatements<?> withStatements = (NodeWithStatements<?>) node;

      elements.addAll(withStatements.getStatements());
    }
    // i.e., int a,b,c
    if (node instanceof NodeWithVariables) {
      NodeWithVariables<?> withVariables = (NodeWithVariables<?>) node;

      elements.add(withVariables.getElementType());
      elements.addAll(withVariables.getVariables());
    }

    if (node instanceof UnionType) {
      UnionType unionType = (UnionType) node;

      elements.addAll(unionType.getElements());
    }

    if (node instanceof ArrayType) {
      ArrayType arrayType = (ArrayType) node;

      elements.add(arrayType.getElementType());
    }

    if (node instanceof TypeParameter) {
      TypeParameter typeParam = (TypeParameter) node;

      elements.addAll(typeParam.getTypeBound());
    }

    if (node instanceof IntersectionType) {
      IntersectionType intersection = (IntersectionType) node;

      elements.addAll(intersection.getElements());
    }

    if (node instanceof WildcardType) {
      WildcardType wildcard = (WildcardType) node;

      if (wildcard.getSuperType().isPresent()) {
        elements.add(wildcard.getSuperType().get());
      }
      if (wildcard.getExtendedType().isPresent()) {
        elements.add(wildcard.getExtendedType().get());
      }
    }

    if (node instanceof LambdaExpr) {
      LambdaExpr lambda = (LambdaExpr) node;

      elements.add(lambda.getBody());
    }

    if (node instanceof EnclosedExpr) {
      EnclosedExpr enclosed = (EnclosedExpr) node;

      elements.add(enclosed.getInner());
    }

    if (node instanceof AssignExpr assign) {
      elements.add(assign.getTarget());
      elements.add(assign.getValue());
    }

    if (node instanceof VariableDeclarator varDecl) {
      if (varDecl.getInitializer().isPresent()) {
        elements.add(varDecl.getInitializer().get());
      }
    }

    if (node instanceof ReturnStmt returnStmt) {
      if (returnStmt.getExpression().isPresent()) {
        elements.add(returnStmt.getExpression().get());
      }
    }

    if (node instanceof IfStmt ifStmt) {
      elements.add(ifStmt.getThenStmt());
      if (ifStmt.getElseStmt().isPresent()) {
        elements.add(ifStmt.getElseStmt().get());
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
      CompilationUnit cu = fqnToCompilationUnits.get(resolvedTypeDeclaration.getQualifiedName());

      if (cu == null) {
        // Not in project; solved by reflection, not our concern
        return elements;
      }

      TypeDeclaration<?> type =
          cu.findFirst(
                  TypeDeclaration.class,
                  n ->
                      n.getFullyQualifiedName().isPresent()
                          && n.getFullyQualifiedName()
                              .get()
                              .equals(resolvedTypeDeclaration.getQualifiedName()))
              .get();
      elements.add(type);
    }

    if (resolved instanceof ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration) {
      String declaringTypeFQN = resolvedMethodLikeDeclaration.declaringType().getQualifiedName();
      CompilationUnit cu = fqnToCompilationUnits.get(declaringTypeFQN);
      if (cu == null) {
        // Not in project; solved by reflection, not our concern
        return elements;
      }

      TypeDeclaration<?> type =
          cu.findFirst(
                  TypeDeclaration.class,
                  n ->
                      n.getFullyQualifiedName().isPresent()
                          && n.getFullyQualifiedName().get().equals(declaringTypeFQN))
              .get();

      Node unattached = resolvedMethodLikeDeclaration.toAst().get();
      CallableDeclaration<?> methodLike =
          type.findFirst(CallableDeclaration.class, n -> n.equals(unattached)).get();

      elements.add(type);
      elements.add(methodLike);
      elements.addAll(getRelevantElements(type));
      elements.addAll(getRelevantElements(methodLike));
    }

    if (resolved instanceof ResolvedFieldDeclaration resolvedFieldDeclaration) {
      String declaringTypeFQN = resolvedFieldDeclaration.declaringType().getQualifiedName();
      CompilationUnit cu = fqnToCompilationUnits.get(declaringTypeFQN);

      if (cu == null) {
        // Not in project; solved by reflection, not our concern
        return elements;
      }

      TypeDeclaration<?> type =
          cu.findFirst(
                  TypeDeclaration.class,
                  n ->
                      n.getFullyQualifiedName().isPresent()
                          && n.getFullyQualifiedName().get().equals(declaringTypeFQN))
              .get();

      Node unattached = resolvedFieldDeclaration.toAst().get();
      FieldDeclaration field =
          type.findFirst(FieldDeclaration.class, n -> n.equals(unattached)).get();

      elements.add(type);
      elements.add(field);
      elements.addAll(getRelevantElements(type));
      elements.addAll(getRelevantElements(field));
    }

    return elements;
  }
}
