package org.checkerframework.specimin;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.Optional;
import java.util.Set;

/**
 * Preserve annotations and their parameter types for used classes. This will only keep annotations
 * if the corresponding class, method, or field declaration is marked to be preserved.
 */
public class AnnotationParameterTypesVisitor extends ModifierVisitor<Void> {
  /** Set containing the signatures of used member (fields and methods). */
  private Set<String> usedMembers;

  /** Set containing the signatures of used classes. */
  private Set<String> usedClass;

  /**
   * Constructs a new SolveMethodOverridingVisitor with the provided sets of target methods, used
   * members, and used classes.
   *
   * @param usedMembers Set containing the signatures of used members.
   * @param usedClass Set containing the signatures of used classes.
   */
  public AnnotationParameterTypesVisitor(Set<String> usedMembers, Set<String> usedClass) {
    this.usedMembers = usedMembers;
    this.usedClass = usedClass;
  }

  @Override
  public Visitable visit(ConstructorDeclaration decl, Void p) {
    if (usedMembers.contains(decl.resolve().getQualifiedSignature())) {
      handleAnnotations(decl.getAnnotations());
    }
    return super.visit(decl, p);
  }

  @Override
  public Visitable visit(MethodDeclaration decl, Void p) {
    if (usedMembers.contains(decl.resolve().getQualifiedSignature())) {
      handleAnnotations(decl.getAnnotations());

      for (Parameter param : decl.getParameters()) {
        handleAnnotations(param.getAnnotations());
      }
    }
    return super.visit(decl, p);
  }

  @Override
  public Visitable visit(FieldDeclaration decl, Void p) {
    String classFullName = decl.resolve().declaringType().getQualifiedName();
    if (usedMembers.contains(classFullName + "#" + decl.resolve().getName())) {
      handleAnnotations(decl.getAnnotations());
    }
    return super.visit(decl, p);
  }

  @Override
  public Visitable visit(ClassOrInterfaceDeclaration decl, Void p) {
    Optional<String> fullyQualified = decl.getFullyQualifiedName();
    if (fullyQualified.isPresent() && usedClass.contains(fullyQualified.get())) {
      handleAnnotations(decl.getAnnotations());
    }
    return super.visit(decl, p);
  }

  @Override
  public Visitable visit(AnnotationDeclaration decl, Void p) {
    Optional<String> fullyQualified = decl.getFullyQualifiedName();
    if (fullyQualified.isPresent() && usedClass.contains(fullyQualified.get())) {
      handleAnnotations(decl.getAnnotations());
    }
    return super.visit(decl, p);
  }

  @Override
  public Visitable visit(EnumDeclaration decl, Void p) {
    Optional<String> fullyQualified = decl.getFullyQualifiedName();
    if (fullyQualified.isPresent() && usedClass.contains(fullyQualified.get())) {
      handleAnnotations(decl.getAnnotations());
    }
    return super.visit(decl, p);
  }

  // We do not use visit() for this visitor since we need the declaration context
  // to determine whether we preserve or not
  /**
   * Helper method to add a list of annotations to the usedClass set, including the types used in
   * annotation parameters.
   *
   * @param annotations The annotations to process
   */
  private void handleAnnotations(NodeList<AnnotationExpr> annotations) {
    for (AnnotationExpr anno : annotations) {
      usedClass.add(anno.resolve().getQualifiedName());
      System.out.println(anno.resolve().getQualifiedName());

      if (anno.isSingleMemberAnnotationExpr()) {
        Expression value = anno.asSingleMemberAnnotationExpr().getMemberValue();
        handleAnnotationValue(value);
      } else if (anno.isNormalAnnotationExpr()) {
        for (MemberValuePair pair : anno.asNormalAnnotationExpr().getPairs()) {
          Expression value = pair.getValue();
          handleAnnotationValue(value);
        }
      }
    }
    System.out.println(usedClass);
    System.out.println(usedMembers);
  }

  /**
   * Handles annotation parameter value types, adding all used types to the usedClass set. This
   * method can handle array types as well as annotations referenced in the parameters. If the type
   * is a primitive, String, or Class, there is no effect.
   */
  private void handleAnnotationValue(Expression value) {
    if (value.isArrayInitializerExpr()) {
      ArrayInitializerExpr array = value.asArrayInitializerExpr();
      NodeList<Expression> values = array.getValues();

      if (values.isEmpty()) {
        return;
      }
      // All elements in the same array will be the same type
      handleAnnotationValue(values.get(0));
    } else if (value.isClassExpr()) {
      ResolvedType resolved = value.asClassExpr().calculateResolvedType();

      if (resolved.isReferenceType()) {
        usedClass.add(resolved.asReferenceType().getQualifiedName());
      }
    } else if (value.isFieldAccessExpr()) {
      ResolvedType resolved = value.asFieldAccessExpr().calculateResolvedType();

      if (resolved.isReferenceType()) {
        usedClass.add(resolved.asReferenceType().getQualifiedName());
      }
    } else if (value.isAnnotationExpr()) {
      // Create a NodeList so we can re-handle the annotation in handleAnnotations
      NodeList<AnnotationExpr> annotation = new NodeList<>();
      annotation.add(value.asAnnotationExpr());

      handleAnnotations(annotation);
    }
  }
}
