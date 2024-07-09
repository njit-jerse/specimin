package org.checkerframework.specimin;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Preserve annotations and their parameter types for used classes. This will only keep annotations
 * if the corresponding class, method, or field declaration is marked to be preserved. If an
 * annotation is not resolvable (including its parameters), it will be removed.
 */
public class AnnotationParameterTypesVisitor extends ModifierVisitor<Void> {
  /** Set containing the signatures of used members (fields and methods). */
  private Set<String> usedMembers;

  /** Set containing the signatures of target methods. */
  private Set<String> targetMethods;

  /** Set containing the signatures of target fields. */
  private List<String> targetFields;

  /** Set containing the signatures of used classes. */
  private Set<String> usedClass;

  /** Set containing the signatures of classes used by annotations. */
  private Set<String> classesToAdd = new HashSet<>();

  /** Map containing the signatures of static imports. */
  Map<String, String> staticImports = new HashMap<>();

  /**
   * Get the set containing the signatures of classes used by annotations.
   *
   * @return The set containing the signatures of classes used by annotations.
   */
  public Set<String> getClassesToAdd() {
    return classesToAdd;
  }

  /**
   * Constructs a new SolveMethodOverridingVisitor with the provided sets of target methods, used
   * members, and used classes.
   *
   * @param targetFields Set containing the signatures of target fields.
   * @param targetMethods Set containing the signatures of target methods.
   * @param usedMembers Set containing the signatures of used members.
   * @param usedClass Set containing the signatures of used classes.
   */
  public AnnotationParameterTypesVisitor(
      List<String> targetFields,
      Set<String> targetMethods,
      Set<String> usedMembers,
      Set<String> usedClass) {
    this.usedMembers = usedMembers;
    this.targetMethods = targetMethods;
    this.targetFields = targetFields;
    this.usedClass = usedClass;
  }

  @Override
  public Node visit(ImportDeclaration decl, Void p) {
    if (decl.isStatic()) {
      String fullName = decl.getNameAsString();
      String memberName = fullName.substring(fullName.lastIndexOf(".") + 1);
      staticImports.put(memberName, fullName);
    }
    return decl;
  }

  @Override
  public Visitable visit(AnnotationMemberDeclaration decl, Void p) {
    // Ensure that enums/fields that are used by default are included
    if (usedClass.contains(PrunerVisitor.getEnclosingClassName(decl))) {
      Optional<Expression> defaultValue = decl.getDefaultValue();
      if (defaultValue.isPresent()) {
        Set<String> usedClassByCurrentAnnotation = new HashSet<>();
        Set<String> usedMembersByCurrentAnnotation = new HashSet<>();
        boolean resolvable =
            handleAnnotationValue(
                defaultValue.get(), usedClassByCurrentAnnotation, usedMembersByCurrentAnnotation);

        if (resolvable) {
          classesToAdd.addAll(usedClassByCurrentAnnotation);
          usedMembers.addAll(usedMembersByCurrentAnnotation);
        }
      }
    }
    return super.visit(decl, p);
  }

  @Override
  public Visitable visit(ConstructorDeclaration decl, Void p) {
    String methodSignature;

    try {
      methodSignature = decl.resolve().getQualifiedSignature();
    } catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
      // UnsupportedOperationException: type is a type variable
      // See TargetMethodFinderVisitor.visit(MethodDeclaration, Void) for more details
      return super.visit(decl, p);
    } catch (RuntimeException e) {
      // The current class is employed by the target methods, although not all of its members are
      // utilized. It's not surprising for unused members to remain unresolved.
      // If this constructor is from the parent of the current class, and it is not resolved, we
      // will get a RuntimeException, otherwise just a UnsolvedSymbolException.
      // From PrunerVisitor.visit(ConstructorDeclaration, Void)
      return decl;
    }

    if (usedMembers.contains(methodSignature) || targetMethods.contains(methodSignature)) {
      handleAnnotations(decl.getAnnotations());
    }
    return super.visit(decl, p);
  }

  @Override
  public Visitable visit(MethodDeclaration decl, Void p) {
    String methodSignature;

    try {
      methodSignature = decl.resolve().getQualifiedSignature();
    } catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
      // UnsupportedOperationException: type is a type variable
      // See TargetMethodFinderVisitor.visit(MethodDeclaration, Void) for more details
      return super.visit(decl, p);
    }
    if (usedMembers.contains(methodSignature) || targetMethods.contains(methodSignature)) {
      handleAnnotations(decl.getAnnotations());

      for (Parameter param : decl.getParameters()) {
        handleAnnotations(param.getAnnotations());
      }
    }
    return super.visit(decl, p);
  }

  @Override
  public Visitable visit(FieldDeclaration decl, Void p) {
    try {
      for (VariableDeclarator var : decl.getVariables()) {
        String qualifiedName =
            PrunerVisitor.getEnclosingClassName(decl) + "#" + var.getNameAsString();
        if (usedMembers.contains(qualifiedName) || targetFields.contains(qualifiedName)) {
          handleAnnotations(decl.getAnnotations());
        }
      }
    } catch (UnsolvedSymbolException ex) {
      return super.visit(decl, p);
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

  // We do not use visit(AnnotationExpr) for this visitor since we need the declaration context
  // to determine whether we preserve or not
  /**
   * Helper method to add a list of annotations to the usedClass set, including the types used in
   * annotation parameters.
   *
   * @param annotations The annotations to process
   */
  private void handleAnnotations(NodeList<AnnotationExpr> annotations) {
    Set<String> usedClassByCurrentAnnotation = new HashSet<>();
    Set<String> usedMembersByCurrentAnnotation = new HashSet<>();
    Set<AnnotationExpr> annosToRemove = new HashSet<>();
    for (AnnotationExpr anno : annotations) {
      boolean resolvable = true;

      if (anno.isSingleMemberAnnotationExpr()) {
        Expression value = anno.asSingleMemberAnnotationExpr().getMemberValue();
        resolvable =
            handleAnnotationValue(
                value, usedClassByCurrentAnnotation, usedMembersByCurrentAnnotation);
      } else if (anno.isNormalAnnotationExpr()) {
        for (MemberValuePair pair : anno.asNormalAnnotationExpr().getPairs()) {
          Expression value = pair.getValue();
          resolvable =
              handleAnnotationValue(
                  value, usedClassByCurrentAnnotation, usedMembersByCurrentAnnotation);
          if (!resolvable) {
            break;
          }
        }
      }

      // Only add annotation to the usedClass set if all parameters are resolvable
      if (resolvable) {
        usedClassByCurrentAnnotation.add(anno.resolve().getQualifiedName());
        classesToAdd.addAll(usedClassByCurrentAnnotation);
        usedMembers.addAll(usedMembersByCurrentAnnotation);
      } else {
        annosToRemove.add(anno);
      }
      usedClassByCurrentAnnotation.clear();
      usedMembersByCurrentAnnotation.clear();
    }

    // Remove unsolvable annotations; these parameter types are unsolvable since
    // the UnsolvedSymbolVisitor did not create synthetic types for annotations
    // included later on
    for (AnnotationExpr anno : annosToRemove) {
      anno.remove();
    }
  }

  /**
   * Handles annotation parameter value types, adding all used types to the usedByCurrentAnnotation
   * set. This method can handle array types as well as annotations referenced in the parameters. If
   * the type is a primitive, String, or Class, there is no effect.
   *
   * @return true if value is resolvable, false if not
   */
  private boolean handleAnnotationValue(
      Expression value,
      Set<String> usedClassByCurrentAnnotation,
      Set<String> usedMembersByCurrentAnnotation) {
    if (value.isArrayInitializerExpr()) {
      ArrayInitializerExpr array = value.asArrayInitializerExpr();
      NodeList<Expression> values = array.getValues();

      if (values.isEmpty()) {
        return true;
      }
      // All elements in the same array will be the same type
      handleAnnotationValue(
          values.get(0), usedClassByCurrentAnnotation, usedMembersByCurrentAnnotation);
      return true;
    } else if (value.isClassExpr()) {
      try {
        ResolvedType resolved = value.asClassExpr().calculateResolvedType();

        if (resolved.isReferenceType()) {
          usedClassByCurrentAnnotation.add(resolved.asReferenceType().getQualifiedName());
        }
      } catch (UnsolvedSymbolException ex) {
        // TODO: retrigger synthetic type generation
        return false;
      }
      return true;
    } else if (value.isFieldAccessExpr()) {
      try {
        ResolvedType resolved = value.asFieldAccessExpr().calculateResolvedType();

        if (resolved.isReferenceType()) {
          String parentName = resolved.asReferenceType().getQualifiedName();
          usedClassByCurrentAnnotation.add(parentName);
          String memberName = value.asFieldAccessExpr().getNameAsString();
          // member here could be an enum or a field
          usedMembersByCurrentAnnotation.add(parentName + "#" + memberName);
          usedMembersByCurrentAnnotation.add(parentName + "." + memberName);
        }
      } catch (UnsolvedSymbolException ex) {
        // TODO: retrigger synthetic type generation
        return false;
      }
      return true;
    } else if (value.isNameExpr()) { // variable of some sort
      try {
        ResolvedType resolved = value.asNameExpr().calculateResolvedType();

        if (resolved.isReferenceType()) {
          usedClassByCurrentAnnotation.add(resolved.asReferenceType().getQualifiedName());
          String fullStaticName = staticImports.get(value.asNameExpr().getNameAsString());
          if (fullStaticName != null) {
            String parentName = fullStaticName.substring(0, fullStaticName.lastIndexOf("."));
            String memberName = fullStaticName.substring(fullStaticName.lastIndexOf(".") + 1);
            // static import here could be an enum or a field
            usedClassByCurrentAnnotation.add(parentName);
            usedMembersByCurrentAnnotation.add(parentName + "#" + memberName);
            usedMembersByCurrentAnnotation.add(fullStaticName);
          }
        }
      } catch (UnsolvedSymbolException ex) {
        // TODO: retrigger synthetic type generation
        return false;
      }
      return true;
    } else if (value.isAnnotationExpr()) {
      // Create a NodeList so we can re-handle the annotation in handleAnnotations
      NodeList<AnnotationExpr> annotation = new NodeList<>();
      annotation.add(value.asAnnotationExpr());

      handleAnnotations(annotation);
      try {
        value.asAnnotationExpr().resolve();
      } catch (UnsolvedSymbolException ex) {
        return false;
      }
      return true;
    }
    return true;
  }
}
