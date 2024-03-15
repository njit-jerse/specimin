package org.checkerframework.specimin;

import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Set;

/**
 * When an enum constant is used inside a target method, we also need to preserve the types relating
 * to the constructor of that enum so the final output can compile.
 */
public class EnumConstructorVisitor extends VoidVisitorAdapter<Void> {

  /** Set of classes used by the target method. */
  private Set<String> usedClass;

  private ArrayDeque<Boolean> insideUsedEnum;

  /**
   * Constructs an EnumConstructorVisitor with the provided set of used members.
   *
   * @param usedClass the provided set of used members.
   */
  public EnumConstructorVisitor(Set<String> usedClass) {
    this.usedClass = usedClass;
    this.insideUsedEnum = new ArrayDeque<>();
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
    if (enumDeclaration.getFullyQualifiedName().isPresent()) {
      if (usedClass.contains(enumDeclaration.getFullyQualifiedName().get())) {
        insideUsedEnum.addFirst(true);
      } else {
        insideUsedEnum.addFirst(false);
      }
      super.visit(enumDeclaration, arg);
      insideUsedEnum.removeFirst();
    }
  }

  @Override
  public void visit(ConstructorDeclaration constructorDeclaration, Void p) {
    boolean currentStatus = insideUsedEnum.getLast();
    if (currentStatus) {
      // as long as the parameter are resolved, the constructor will be preserved.
      for (Parameter parameter : constructorDeclaration.getParameters()) {
        TargetMethodFinderVisitor.updateUsedClassBasedOnType(
            parameter.getType().resolve(), usedClass, new HashMap<>());
      }
    }
    super.visit(constructorDeclaration, p);
  }
}
