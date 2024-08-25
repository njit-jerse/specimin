package org.checkerframework.specimin;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Removes all unnecessary ElementType values from each annotation declaration, based on their
 * usages. Run this visitor after PrunerVisitor to ensure all unnecessary ElementTypes are removed.
 */
public class AnnotationTargetRemoverVisitor extends ModifierVisitor<Void> {
  /** A Map of fully qualified annotation names to its ElementTypes. */
  private final Map<String, Set<ElementType>> annotationToElementTypes = new HashMap<>();

  /** A Map of fully qualified annotation names to their declarations. */
  private final Map<String, AnnotationDeclaration> annotationToDeclaration = new HashMap<>();

  private static final String TARGET_PACKAGE = "java.lang.annotation";
  private static final String TARGET_NAME = "Target";
  private static final String FULLY_QUALIFIED_TARGET =  TARGET_PACKAGE + "." + TARGET_NAME;

  /**
   * Updates all annotation declaration {@code @Target} values to match their usages. Only removes
   * {@code ElementType}s, doesn't add them. Call this method once after visiting all files.
   */
  public void removeExtraAnnotationTargets() {
    for (Map.Entry<String, AnnotationDeclaration> pair : annotationToDeclaration.entrySet()) {
      AnnotationExpr targetAnnotation = pair.getValue().getAnnotationByName(TARGET_NAME).orElse(null);

      if (targetAnnotation == null) {
        // This is most likely an existing definition. If there is no @Target annotation,
        // we shouldn't add it
        continue;
      }

      boolean useFullyQualified =
          targetAnnotation.getNameAsString().equals(FULLY_QUALIFIED_TARGET);

      // Only handle java.lang.annotation.Target
      if (!targetAnnotation.resolve().getQualifiedName().equals(FULLY_QUALIFIED_TARGET)) {
        continue;
      }

      Set<ElementType> actualElementTypes = annotationToElementTypes.get(pair.getKey());

      if (actualElementTypes == null) {
        // No usages of the annotation itself (see Issue272Test)
        actualElementTypes = new HashSet<>();
      }

      Set<ElementType> elementTypes = new HashSet<>();
      Set<ElementType> staticallyImportedElementTypes = new HashSet<>();
      Expression memberValue;

      // @Target(ElementType.___)
      // @Target({ElementType.___, ElementType.___})
      if (targetAnnotation.isSingleMemberAnnotationExpr()) {
        SingleMemberAnnotationExpr asSingleMember = targetAnnotation.asSingleMemberAnnotationExpr();
        memberValue = asSingleMember.getMemberValue();
      }
      // @Target(value = ElementType.___)
      // @Target(value = {ElementType.___, ElementType.___})
      else if (targetAnnotation.isNormalAnnotationExpr()) {
        NormalAnnotationExpr asNormal = targetAnnotation.asNormalAnnotationExpr();
        memberValue = asNormal.getPairs().get(0).getValue();
      } else {
        throw new RuntimeException("@Target annotation must contain an ElementType");
      }

      // If there's only one ElementType, we can't remove anything
      // We should only be removing ElementTypes, not adding: we do not want to
      // convert a non TYPE_USE annotation to a TYPE_USE annotation, for example
      if (memberValue.isFieldAccessExpr() || memberValue.isNameExpr()) {
        continue;
      } else if (memberValue.isArrayInitializerExpr()) {
        ArrayInitializerExpr arrayExpr = memberValue.asArrayInitializerExpr();
        if (arrayExpr.getValues().size() <= 1) {
          continue;
        }
        for (Expression value : arrayExpr.getValues()) {
          if (value.isFieldAccessExpr()) {
            FieldAccessExpr fieldAccessExpr = value.asFieldAccessExpr();
            ElementType elementType = ElementType.valueOf(fieldAccessExpr.getNameAsString());
            if (actualElementTypes.contains(elementType)) {
              elementTypes.add(elementType);
            }
          } else if (value.isNameExpr()) {
            // In case of static imports
            NameExpr nameExpr = value.asNameExpr();
            ElementType elementType = ElementType.valueOf(nameExpr.getNameAsString());
            if (actualElementTypes.contains(elementType)) {
              elementTypes.add(elementType);
              staticallyImportedElementTypes.add(elementType);
            }
          }
        }
      }

      StringBuilder newAnnotation = new StringBuilder();
      newAnnotation.append("@");

      if (useFullyQualified) {
        newAnnotation.append(TARGET_PACKAGE);
        newAnnotation.append(".");
      }

      newAnnotation.append(TARGET_NAME);
      newAnnotation.append("(");

      newAnnotation.append('{');

      List<ElementType> sortedElementTypes = new ArrayList<>(elementTypes);
      Collections.sort(sortedElementTypes, (a, b) -> a.name().compareTo(b.name()));

      for (int i = 0; i < sortedElementTypes.size(); i++) {
        ElementType elementType = sortedElementTypes.get(i);
        if (!staticallyImportedElementTypes.contains(elementType)) {
          if (useFullyQualified) {
            newAnnotation.append(TARGET_PACKAGE);
            newAnnotation.append(".");
          }
          newAnnotation.append("ElementType.");
        }
        newAnnotation.append(elementType.name());

        if (i < sortedElementTypes.size() - 1) {
          newAnnotation.append(", ");
        }
      }

      newAnnotation.append("})");
      AnnotationExpr trimmed = StaticJavaParser.parseAnnotation(newAnnotation.toString());

      targetAnnotation.remove();

      pair.getValue().addAnnotation(trimmed);
    }
  }

  @Override
  public Visitable visit(AnnotationDeclaration decl, Void p) {
    annotationToDeclaration.put(decl.getFullyQualifiedName().get(), decl);
    return super.visit(decl, p);
  }

  @Override
  public Visitable visit(MarkerAnnotationExpr anno, Void p) {
    updateAnnotationElementTypes(anno);
    return super.visit(anno, p);
  }

  @Override
  public Visitable visit(NormalAnnotationExpr anno, Void p) {
    updateAnnotationElementTypes(anno);
    return super.visit(anno, p);
  }

  @Override
  public Visitable visit(SingleMemberAnnotationExpr anno, Void p) {
    updateAnnotationElementTypes(anno);
    return super.visit(anno, p);
  }

  /**
   * Helper method to update the ElementTypes for an annotation.
   *
   * @param anno The annotation to update element types for
   */
  private void updateAnnotationElementTypes(AnnotationExpr anno) {
    Node parent = anno.getParentNode().orElse(null);

    if (parent == null) {
      return;
    }

    Set<ElementType> elementTypes = annotationToElementTypes.get(anno.resolve().getQualifiedName());
    if (elementTypes == null) {
      elementTypes = new HashSet<>();
      annotationToElementTypes.put(anno.resolve().getQualifiedName(), elementTypes);
    }

    if (parent instanceof AnnotationDeclaration) {
      elementTypes.add(ElementType.ANNOTATION_TYPE);
      elementTypes.add(ElementType.TYPE);
    } else if (parent instanceof ConstructorDeclaration) {
      elementTypes.add(ElementType.CONSTRUCTOR);
    } else if (parent instanceof FieldDeclaration || parent instanceof EnumConstantDeclaration) {
      elementTypes.add(ElementType.FIELD);
    } else if (parent instanceof VariableDeclarationExpr) {
      elementTypes.add(ElementType.LOCAL_VARIABLE);
    } else if (parent instanceof MethodDeclaration) {
      elementTypes.add(ElementType.METHOD);

      if (((MethodDeclaration) parent).getType().isVoidType()) {
        // If it's void we don't need to add TYPE_USE
        return;
      }
    } else if (parent instanceof AnnotationMemberDeclaration) {
      elementTypes.add(ElementType.METHOD);
    } else if (parent instanceof PackageDeclaration) {
      elementTypes.add(ElementType.PACKAGE);
      return;
    } else if (parent instanceof Parameter) {
      if (parent.getParentNode().isPresent()
          && parent.getParentNode().get() instanceof RecordDeclaration) {
        elementTypes.add(ElementType.RECORD_COMPONENT);
      } else {
        elementTypes.add(ElementType.PARAMETER);
      }
    } else if (parent instanceof TypeDeclaration) {
      // TypeDeclaration is the parent class for class, interface, annotation, record declarations
      // https://www.javadoc.io/doc/com.github.javaparser/javaparser-core/latest/com/github/javaparser/ast/body/TypeDeclaration.html
      elementTypes.add(ElementType.TYPE);
    } else if (parent instanceof TypeParameter) {
      elementTypes.add(ElementType.TYPE_PARAMETER);
    }

    elementTypes.add(ElementType.TYPE_USE);
  }
}
