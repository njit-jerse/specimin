package org.checkerframework.specimin;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
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
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.DefaultConstructorDeclaration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

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
      elements.addAll(withAnnotations.getAnnotations());
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
      TypeDeclaration<?> type =
          getTypeFromQualifiedName(resolvedTypeDeclaration.getQualifiedName());

      if (type == null) return elements;
      elements.add(type);
    }

    if (resolved instanceof ResolvedMethodLikeDeclaration resolvedMethodLikeDeclaration) {
      TypeDeclaration<?> type =
          getTypeFromQualifiedName(
              resolvedMethodLikeDeclaration.declaringType().getQualifiedName());
      if (type == null) return elements;

      if (resolved instanceof ResolvedMethodDeclaration resolvedMethodDeclaration) {
        // If this current method is an override, add the original definition too
        // Get direct ancestors, since each ancestor method definition will return to this method.
        List<ClassOrInterfaceType> parents = new ArrayList<>();

        if (type instanceof NodeWithExtends<?> withExtends) {
          parents.addAll(withExtends.getExtendedTypes());
        }
        if (type instanceof NodeWithImplements<?> withImplements) {
          parents.addAll(withImplements.getImplementedTypes());
        }

        for (ClassOrInterfaceType parent : parents) {
          try {
            ResolvedType parentType = parent.resolve();

            if (!parentType.isReferenceType()
                || parentType.asReferenceType().getTypeDeclaration().isEmpty()) {
              continue;
            }

            ResolvedReferenceTypeDeclaration decl =
                parentType.asReferenceType().getTypeDeclaration().get();

            TypeDeclaration<?> typeDecl = getTypeFromQualifiedName(decl.getQualifiedName());
            if (typeDecl == null) continue;

            for (ResolvedMethodDeclaration method : decl.asReferenceType().getDeclaredMethods()) {
              if (resolvedMethodDeclaration.getSignature().equals(method.getSignature())) {
                Node unattached = method.toAst().get();
                MethodDeclaration methodDecl =
                    typeDecl.findFirst(MethodDeclaration.class, n -> n.equals(unattached)).get();

                elements.add(methodDecl);
              }
            }
          } catch (UnsolvedSymbolException ex) {
            // continue
          }
        }
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
          getTypeFromQualifiedName(resolvedFieldDeclaration.declaringType().getQualifiedName());

      if (type == null) return elements;

      Node unattached = resolvedFieldDeclaration.toAst().get();
      FieldDeclaration field =
          type.findFirst(FieldDeclaration.class, n -> n.equals(unattached)).get();

      elements.add(type);
      elements.add(field);
    }

    return elements;
  }

  /**
   * Gets the corresponding type declaration from a qualified type name.
   *
   * @param fqn The fully-qualified type name
   * @return The type declaration
   */
  private @Nullable TypeDeclaration<?> getTypeFromQualifiedName(String fqn) {
    CompilationUnit cu = fqnToCompilationUnits.get(fqn);

    if (cu == null) {
      // Not in project; solved by reflection, not our concern
      return null;
    }

    TypeDeclaration<?> type =
        cu.findFirst(
                TypeDeclaration.class,
                n ->
                    n.getFullyQualifiedName().isPresent()
                        && n.getFullyQualifiedName().get().equals(fqn))
            .get();

    return type;
  }
}
