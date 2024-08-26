package org.checkerframework.specimin;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionAnnotationDeclaration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Preserve annotations and their parameter types for used classes. This will only keep annotations
 * if the corresponding class, method, or field declaration is marked to be preserved. If an
 * annotation (or its parameters) is not resolvable, it will be removed.
 */
public class AnnotationParameterTypesVisitor extends SpeciminStateVisitor {
  /**
   * Constructs a new AnnotationParameterTypesVisitor with the previous visitor
   *
   * @param previousVisitor the last visitor to run before this one
   */
  public AnnotationParameterTypesVisitor(SpeciminStateVisitor previousVisitor) {
    super(previousVisitor);
  }

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
    // Also, preserve method type since a definition with a default value may be
    // added, but that value type is never explored by the visit(AnnotationExpr) methods
    // For example, when the type is an enum (Foo), the definition may set a default value to
    // Foo.VALUE, but Foo.VALUE may never be referenced in an @Annotation() usage (instead,
    // other Foo values may) be used, so Foo.VALUE would be removed by PrunerVisitor and result
    // in compile errors.
    if (usedTypeElements.contains(JavaParserUtil.getEnclosingClassName(decl))) {
      // Class<> from jar files may contain other classes
      if (decl.getType().toString().startsWith("Class<")) {
        // Replace with Class<?> to prevent compile-time errors
        String type = "Class<?>";
        if (decl.getType().isArrayType()) {
          type += "[]";
        }
        decl.setType(type);
      } else {
        try {
          ResolvedType resolved = decl.getType().resolve();
          if (resolved.isArray()) {
            resolved = resolved.asArrayType().getComponentType();
          }
          if (resolved.isReferenceType()) {
            usedTypeElements.add(resolved.asReferenceType().getQualifiedName());
          }
        } catch (UnsolvedSymbolException ex) {
          // TODO: retrigger synthetic type generation
          return super.visit(decl, p);
        }
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
    Node parent = JavaParserUtil.findClosestParentMemberOrClassLike(anno);

    if (isTargetOrUsed(parent)) {
      handleAnnotation(anno);
    }
    return super.visit(anno, p);
  }

  @Override
  public Visitable visit(SingleMemberAnnotationExpr anno, Void p) {
    Node parent = JavaParserUtil.findClosestParentMemberOrClassLike(anno);

    if (isTargetOrUsed(parent)) {
      handleAnnotation(anno);
    }
    return super.visit(anno, p);
  }

  @Override
  public Visitable visit(NormalAnnotationExpr anno, Void p) {
    Node parent = JavaParserUtil.findClosestParentMemberOrClassLike(anno);

    if (isTargetOrUsed(parent)) {
      handleAnnotation(anno);
    }
    return super.visit(anno, p);
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
          && !JavaLangUtils.inJdkPackage(qualifiedName)) {
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
   * the type is a primitive or String, there is no effect.
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
      for (Expression val : values) {
        handleAnnotationValue(val, usedClassByCurrentAnnotation, usedMembersByCurrentAnnotation);
      }
      return true;
    } else if (value.isClassExpr()) {
      try {
        ResolvedType resolved = value.asClassExpr().getType().resolve();

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
