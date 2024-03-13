package org.checkerframework.specimin;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import java.util.HashSet;
import java.util.Set;

/**
 * This is an auxiliary visitor for TargetMethodFinderVisitor. This InheritancePreserveVisitor makes
 * sure that every file belonging to the inheritance chain of a used class is also marked as used.
 */
public class InheritancePreserveVisitor extends ModifierVisitor<Void> {

  /** List of classes used by the target methods. */
  public Set<String> usedClass;

  /** List of fully-qualified classnames to be added to the list of used classes. */
  public Set<String> addedClasses = new HashSet<>();

  /**
   * Constructs an InheritancePreserveVisitor with the specified set of used classes.
   *
   * @param usedClass The set of classes used by the target methods.
   */
  public InheritancePreserveVisitor(Set<String> usedClass) {
    this.usedClass = usedClass;
  }

  /**
   * Return the set of classes to be added to the list of used classes.
   *
   * @return The value of addedClasses.
   */
  public Set<String> getAddedClasses() {
    Set<String> copyOfAddedClasses = new HashSet<>();
    copyOfAddedClasses.addAll(addedClasses);
    return copyOfAddedClasses;
  }

  /** Empty the list of added classes. */
  public void emptyAddedClasses() {
    addedClasses = new HashSet<>();
  }

  @Override
  public Visitable visit(ClassOrInterfaceDeclaration decl, Void p) {
    if (usedClass.contains(decl.resolve().getQualifiedName())) {
      for (ClassOrInterfaceType extendedType : decl.getExtendedTypes()) {
        String qualifiedName = extendedType.resolve().getQualifiedName();
        addedClasses.add(qualifiedName);
      }
    }
    return super.visit(decl, p);
  }
}
