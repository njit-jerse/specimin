package org.checkerframework.specimin;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.HashMap;
import java.util.Set;

/**
 * When an enum constant is used inside a target method, we also need to preserve the types relating
 * to the constructor of that enum so the final output can compile.
 */
public class EnumConstructorVisitor extends VoidVisitorAdapter<Void> {

  /** Set of classes used by the target method. */
  private Set<String> usedClass;

  /** Check whether the visitor is inside an enum used by target methods. */
  private boolean insideUsedEnum = false;

  /**
   * Constructs an EnumConstructorVisitor with the provided set of used members.
   *
   * @param usedClass the provided set of used members.
   */
  public EnumConstructorVisitor(Set<String> usedClass) {
    this.usedClass = usedClass;
  }

  /**
   * Get the set of used members.
   *
   * @return the set of used members.
   */
  public Set<String> getUsedClass() {
    return usedClass;
  }

  @Override
  public void visit(EnumDeclaration enumDeclaration, Void arg) {
    boolean oldInsideUsedEnum = insideUsedEnum;
    if (enumDeclaration.getFullyQualifiedName().isPresent()) {
      if (usedClass.contains(enumDeclaration.getFullyQualifiedName().get())) {
        insideUsedEnum = true;
      } else {
        insideUsedEnum = false;
      }
      super.visit(enumDeclaration, arg);
      insideUsedEnum = oldInsideUsedEnum;
    }
  }

  @Override
  public void visit(ConstructorDeclaration constructorDeclaration, Void p) {
    if (insideUsedEnum) {
      // as long as the parameter are resolved, the constructor will be preserved.
      for (Parameter parameter : constructorDeclaration.getParameters()) {
        TargetMethodFinderVisitor.updateUsedClassBasedOnType(
            parameter.getType().resolve(), usedClass, new HashMap<>());
      }
    }
    super.visit(constructorDeclaration, p);
  }
}
