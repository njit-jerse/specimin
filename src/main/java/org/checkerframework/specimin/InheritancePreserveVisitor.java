package org.checkerframework.specimin;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
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
        try {
          updateAddedClassWithQualifiedClassName(extendedType.resolve().getQualifiedName());
          if (extendedType.getTypeArguments().isPresent()) {
            for (Type typeAgrument : extendedType.getTypeArguments().get()) {
              updateAddedClassWithQualifiedClassName(typeAgrument.resolve().describe());
            }
          }
        }
        // since Specimin does not create synthetic inheritance for interfaces.
        catch (UnsolvedSymbolException | UnsupportedOperationException e) {
          continue;
        }
      }
    }
    return super.visit(decl, p);
  }

  /**
   * (This method is copied from TargetMethodFinderVisitor).
   *
   * <p>Updates the list of used classes with the given qualified class name and its corresponding
   * primary classes and enclosing classes. This includes cases such as classes not sharing the same
   * name as their Java files or nested classes.
   *
   * @param qualifiedClassName The qualified class name to be included in the list of used classes.
   */
  public void updateAddedClassWithQualifiedClassName(String qualifiedClassName) {
    // in case of type variables
    if (!qualifiedClassName.contains(".")) {
      return;
    }
    // strip type variables, if they're present
    if (qualifiedClassName.contains("<")) {
      qualifiedClassName = qualifiedClassName.substring(0, qualifiedClassName.indexOf("<"));
    }
    addedClasses.add(qualifiedClassName);

    String potentialOuterClass =
        qualifiedClassName.substring(0, qualifiedClassName.lastIndexOf("."));
    if (UnsolvedSymbolVisitor.isAClassPath(potentialOuterClass)) {
      updateAddedClassWithQualifiedClassName(potentialOuterClass);
    }
  }
}
