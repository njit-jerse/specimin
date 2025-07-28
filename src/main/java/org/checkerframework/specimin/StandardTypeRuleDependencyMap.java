package org.checkerframework.specimin;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
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
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.DefaultConstructorDeclaration;
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

    // Statements
    // ** If a statement is included in the slice, then that means it is in one
    // of the target members. Therefore, its children are always relevant.

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

      // Rare case: new Foo() but Foo does not contain a constructor
      if (!(resolved instanceof DefaultConstructorDeclaration)) {
        Node unattached = resolvedMethodLikeDeclaration.toAst().get();
        CallableDeclaration<?> methodLike =
            type.findFirst(CallableDeclaration.class, n -> n.equals(unattached)).get();

        elements.add(methodLike);
        elements.addAll(getRelevantElements(methodLike));
      }

      elements.add(type);
      elements.addAll(getRelevantElements(type));
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
