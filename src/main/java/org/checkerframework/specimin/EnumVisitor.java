package org.checkerframework.specimin;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;

/**
 * This visitor updates the list of used classes based on the enum constants used inside the target
 * methods.
 */
public class EnumVisitor extends SpeciminStateVisitor {

  /**
   * Constructor matching super.
   *
   * @param previous the previous Specimin visitor
   */
  public EnumVisitor(SpeciminStateVisitor previous) {
    super(previous);
  }

  @Override
  public Visitable visit(MethodDeclaration methodDeclaration, Void arg) {
    String methodQualifiedSignature =
        this.currentClassQualifiedName
            + "#"
            + SpeciminStateVisitor.removeMethodReturnTypeAndAnnotations(
                methodDeclaration.getDeclarationAsString(false, false, false));
    if (targetMethods.contains(methodQualifiedSignature)) {
      return super.visit(methodDeclaration, arg);
    }
    return methodDeclaration;
    // no need to visit non-target methods.
  }

  @Override
  public Visitable visit(FieldAccessExpr fieldAccessExpr, Void arg) {
    if (insideTargetMember) {
      updateUsedEnumForPotentialEnum(fieldAccessExpr);
    }
    return super.visit(fieldAccessExpr, arg);
  }

  @Override
  public Visitable visit(NameExpr nameExpr, Void arg) {
    if (insideTargetMember) {
      updateUsedEnumForPotentialEnum(nameExpr);
    }
    return super.visit(nameExpr, arg);
  }

  /**
   * Given an expression that could be an enum, this method updates the list of used enums
   * accordingly.
   *
   * @param expression an expression that could be an enum.
   */
  public void updateUsedEnumForPotentialEnum(Expression expression) {
    ResolvedValueDeclaration resolvedField;
    // JavaParser sometimes consider an enum usage a field access expression, sometimes a name
    // expression.
    if (expression.isFieldAccessExpr()) {
      try {
        resolvedField = expression.asFieldAccessExpr().resolve();
      } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
        return;
      }
    } else if (expression.isNameExpr()) {
      try {
        resolvedField = expression.asNameExpr().resolve();
      } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
        return;
      }
    } else {
      throw new RuntimeException(
          "Unexpected parameter for updateUsedClassForPotentialEnum: " + expression);
    }

    if (resolvedField.isEnumConstant()) {
      ResolvedType correspondingEnumDeclaration = resolvedField.asEnumConstant().getType();
      usedTypeElements.add(correspondingEnumDeclaration.describe());
    }
  }
}
