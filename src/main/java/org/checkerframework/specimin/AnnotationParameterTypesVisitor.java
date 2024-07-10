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
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionAnnotationDeclaration;
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
      // Class<> from jar files may contain other classes
      if (decl.getType().toString().startsWith("Class<")) {
        // Replace with Class<?> to prevent compile-time errors
        String type = "Class<?>";
        if (decl.getType().isArrayType()) {
          type += "[]";
        }
        decl.setType(type);
      }
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
  public Visitable visit(MarkerAnnotationExpr anno, Void p) {
    Node parent = findClosestParentMemberOrClassLike(anno);

    if (isTargetOrUsed(parent)) {
      handleAnnotation(anno);
    }
    return super.visit(anno, p);
  }

  @Override
  public Visitable visit(SingleMemberAnnotationExpr anno, Void p) {
    Node parent = findClosestParentMemberOrClassLike(anno);

    if (isTargetOrUsed(parent)) {
      handleAnnotation(anno);
    }
    return super.visit(anno, p);
  }

  @Override
  public Visitable visit(NormalAnnotationExpr anno, Void p) {
    Node parent = findClosestParentMemberOrClassLike(anno);

    if (isTargetOrUsed(parent)) {
      handleAnnotation(anno);
    }
    return super.visit(anno, p);
  }

  /**
   * Finds the closest method, field, or class-like declaration (enums, annos)
   *
   * @param node The node to find the parent for
   * @return the Node of the closest member or class declaration
   */
  public Node findClosestParentMemberOrClassLike(Node node) {
    Node parent = node.getParentNode().orElseThrow();
    while (!(parent instanceof ClassOrInterfaceDeclaration
        || parent instanceof EnumDeclaration
        || parent instanceof AnnotationDeclaration
        || parent instanceof ConstructorDeclaration
        || parent instanceof MethodDeclaration
        || parent instanceof FieldDeclaration)) {
      parent = parent.getParentNode().orElseThrow();
    }
    return parent;
  }

  /**
   * Determines if the given Node is a target/used method or class. Should be used following
   * findClosestParentMemberOrClassLike().
   *
   * @param node The node to check
   */
  private boolean isTargetOrUsed(Node node) {
    String qualifiedName;
    boolean isClass = false;
    if (node instanceof ClassOrInterfaceDeclaration) {
      Optional<String> qualifiedNameOptional =
          ((ClassOrInterfaceDeclaration) node).getFullyQualifiedName();
      if (qualifiedNameOptional.isEmpty()) {
        return false;
      }
      qualifiedName = qualifiedNameOptional.get();
      isClass = true;
    } else if (node instanceof EnumDeclaration) {
      Optional<String> qualifiedNameOptional = ((EnumDeclaration) node).getFullyQualifiedName();
      if (qualifiedNameOptional.isEmpty()) {
        return false;
      }
      qualifiedName = qualifiedNameOptional.get();
      isClass = true;
    } else if (node instanceof AnnotationDeclaration) {
      Optional<String> qualifiedNameOptional =
          ((AnnotationDeclaration) node).getFullyQualifiedName();
      if (qualifiedNameOptional.isEmpty()) {
        return false;
      }
      qualifiedName = qualifiedNameOptional.get();
      isClass = true;
    } else if (node instanceof ConstructorDeclaration) {
      try {
        qualifiedName = ((ConstructorDeclaration) node).resolve().getQualifiedSignature();
      } catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
        // UnsupportedOperationException: type is a type variable
        // See TargetMethodFinderVisitor.visit(MethodDeclaration, Void) for more details
        return false;
      } catch (RuntimeException e) {
        // The current class is employed by the target methods, although not all of its members are
        // utilized. It's not surprising for unused members to remain unresolved.
        // If this constructor is from the parent of the current class, and it is not resolved, we
        // will get a RuntimeException, otherwise just a UnsolvedSymbolException.
        // From PrunerVisitor.visit(ConstructorDeclaration, Void)
        return false;
      }
    } else if (node instanceof MethodDeclaration) {
      try {
        qualifiedName = ((MethodDeclaration) node).resolve().getQualifiedSignature();
      } catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
        // UnsupportedOperationException: type is a type variable
        // See TargetMethodFinderVisitor.visit(MethodDeclaration, Void) for more details
        return false;
      }
    } else if (node instanceof FieldDeclaration) {
      try {
        FieldDeclaration decl = (FieldDeclaration) node;
        for (VariableDeclarator var : decl.getVariables()) {
          qualifiedName = PrunerVisitor.getEnclosingClassName(decl) + "#" + var.getNameAsString();
          if (usedMembers.contains(qualifiedName) || targetFields.contains(qualifiedName)) {
            return true;
          }
        }
      } catch (UnsolvedSymbolException ex) {
        return false;
      }
      return false;
    } else {
      return false;
    }

    if (isClass) {
      return usedClass.contains(qualifiedName);
    } else {
      // fields should already be handled at this point
      return usedMembers.contains(qualifiedName) || targetMethods.contains(qualifiedName);
    }
  }

  /**
   * Helper method to add an annotation to the usedClass set, including the types used in annotation
   * parameters.
   *
   * @param anno The annotation to process
   */
  private void handleAnnotation(AnnotationExpr anno) {
    Set<String> usedClassByCurrentAnnotation = new HashSet<>();
    Set<String> usedMembersByCurrentAnnotation = new HashSet<>();
    boolean resolvable = true;
    try {
      String qualifiedName = anno.resolve().getQualifiedName();
      if (anno.resolve() instanceof ReflectionAnnotationDeclaration
          && !qualifiedName.startsWith("java.lang")) {
        // This usually means that JavaParser has resolved this through the import, but there
        // is no file/CompilationUnit behind it, so we should discard it to prevent compile errors
        anno.remove();
        return;
      }
    } catch (UnsolvedSymbolException ex) {
      anno.remove();
      return;
    }

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
      // Remove unsolvable annotations; these parameter types are unsolvable since
      // the UnsolvedSymbolVisitor did not create synthetic types for annotations
      // included later on
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
    }
    return true;
  }
}
