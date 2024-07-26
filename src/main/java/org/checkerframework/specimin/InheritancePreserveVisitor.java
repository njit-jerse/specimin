package org.checkerframework.specimin;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import java.util.HashMap;
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
    for (String addedClass : addedClasses) {
      // An interface might be added to the list of added class infinitely. Consider this example:
      // public interface Baz extends Comparable<Baz>{}
      // If we don't have the condition check below, 'Baz' would be repeatedly added to the list of
      // added classes every time it's visited, leading to an infinite loop.
      if (usedClass.contains(addedClass)) {
        continue;
      }
      copyOfAddedClasses.add(addedClass);
    }
    return copyOfAddedClasses;
  }

  /** Empty the list of added classes. */
  public void emptyAddedClasses() {
    addedClasses = new HashSet<>();
  }

  /** Cheap and dirty trick to avoid an infinite loop TODO: clean this up after the deadline */
  private static HashSet<String> visitedBounds = new HashSet();

  @Override
  public Visitable visit(ClassOrInterfaceDeclaration decl, Void p) {
    if (usedClass.contains(decl.resolve().getQualifiedName())) {
      if (decl.getTypeParameters().size() > 0) {
        // preserve the bounds of the type parameters, too
        for (TypeParameter tp : decl.getTypeParameters()) {
          for (Type bound : tp.getTypeBound()) {
            String boundDesc = bound.resolve().describe();
            if (visitedBounds.add(boundDesc)) {
              TargetMemberFinderVisitor.updateUsedClassWithQualifiedClassName(
                  boundDesc, addedClasses, new HashMap<>());
            }
          }
        }
      }

      for (ClassOrInterfaceType extendedType : decl.getExtendedTypes()) {
        try {
          // Including a non-primary to primary map in this context may lead to an infinite loop,
          // especially if the superclass is nested within the current class file, resulting in
          // infinite file visits. The TargetMethodFinderVisitor already addresses the updating job
          // in such cases. (Refer to the SuperClass test for an example.)
          TargetMemberFinderVisitor.updateUsedClassWithQualifiedClassName(
              extendedType.resolve().describe(), addedClasses, new HashMap<>());
          if (extendedType.getTypeArguments().isPresent()) {
            for (Type typeArgument : extendedType.getTypeArguments().get()) {
              TargetMemberFinderVisitor.updateUsedClassWithQualifiedClassName(
                  typeArgument.resolve().describe(), addedClasses, new HashMap<>());
            }
          }
        } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
          continue;
        }
      }

      for (ClassOrInterfaceType implementedType : decl.getImplementedTypes()) {
        try {
          String interfacename = implementedType.resolve().describe();
          if (JavaLangUtils.inJdkPackage(interfacename)) {
            // Avoid keeping implementations of java.* classes, because those
            // would require us to actually implement them (we can't remove things
            // from their definitions). This might technically break our guarantees, but it works
            // in practice. TODO: fix this up
            continue;
          }
          TargetMemberFinderVisitor.updateUsedClassWithQualifiedClassName(
              interfacename, addedClasses, new HashMap<>());
          if (implementedType.getTypeArguments().isPresent()) {
            for (Type typeAgrument : implementedType.getTypeArguments().get()) {
              TargetMemberFinderVisitor.updateUsedClassWithQualifiedClassName(
                  typeAgrument.resolve().describe(), addedClasses, new HashMap<>());
            }
          }
        } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
          continue;
        }
      }
    }
    return super.visit(decl, p);
  }
}
