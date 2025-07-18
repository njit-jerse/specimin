package org.checkerframework.specimin.unsolved;

import java.util.HashSet;
import java.util.Set;

/**
 * Given a set of fully qualified type name, hold all possible type definitions. For example, if
 * given a set containing FQNs like org.example.Apple.Banana, generate alternates where it can be
 * any of the following:
 *
 * <ul>
 *   <li>class Banana in package org.example.Apple
 *   <li>inner class Banana in class Apple in package org.example
 *   <li>inner class Banana in inner class Apple in class example in package org
 * </ul>
 */
public class UnsolvedClassOrInterfaceAlternates
    extends UnsolvedSymbolAlternates<UnsolvedClassOrInterface> {

  private Set<String> fullyQualifiedNames = new HashSet<>();

  private UnsolvedClassOrInterfaceAlternates() {}

  public static UnsolvedClassOrInterfaceAlternates create(Set<String> fqns) {
    UnsolvedClassOrInterfaceAlternates result = new UnsolvedClassOrInterfaceAlternates();

    for (String fqn : fqns) {
      // In org.example.Class.Class2, we go from Class2 --> Class.Class2 --> example.Class.Class2
      String packageName = fqn.substring(0, fqn.indexOf('.'));
      UnsolvedClassOrInterface last = null;

      do {
        UnsolvedClassOrInterface type =
            new UnsolvedClassOrInterface(
                fqn.substring(packageName.length() + 1), packageName, last != null);

        if (last != null) {
          // Last represents the child of the new class
          type.addInnerClass(last.copy());
        }

        result.addAlternate(type);
        last = type;
      } while (packageName.contains("."));
    }

    return result;
  }

  /**
   * Given an updated set of potential fully-qualified names, this method finds the intersection of
   * the two sets and updates the existing set.
   *
   * @param updated The additional set
   */
  public void updateFullyQualifiedNames(Set<String> updated) {
    // Update in-place; intersection = removing all elements in the original set
    // that isn't found in the updated set
    getAlternates()
        .removeIf(alternate -> !fullyQualifiedNames.contains(alternate.getFullyQualifiedName()));
    fullyQualifiedNames.removeIf(name -> updated.contains(name));
  }

  public Set<String> getFullyQualifiedNames() {
    return fullyQualifiedNames;
  }

  @Override
  public void addAlternate(UnsolvedClassOrInterface alternate) {
    super.addAlternate(alternate);
    this.fullyQualifiedNames.add(alternate.getFullyQualifiedName());
  }

  public boolean isAnInterface() {
    // All alternates are either classes or interfaces
    return getAlternates().get(0).isAnInterface();
  }

  public void setIsAnInterfaceToTrue() {
    for (UnsolvedClassOrInterface alternate : getAlternates()) {
      alternate.setIsAnInterfaceToTrue();
    }
  }

  public void setIsAnAnnotationToTrue() {
    for (UnsolvedClassOrInterface alternate : getAlternates()) {
      boolean orig = alternate.isAnAnnotation();
      alternate.setIsAnAnnotationToTrue();

      if (!orig) {
        UnsolvedClassOrInterface copy = alternate.copy();
        alternate.addAnnotation(
            "@java.lang.annotation.Target({ java.lang.annotation.ElementType.TYPE,"
                + " java.lang.annotation.ElementType.FIELD,"
                + " java.lang.annotation.ElementType.METHOD,"
                + " java.lang.annotation.ElementType.PARAMETER,"
                + " java.lang.annotation.ElementType.CONSTRUCTOR,"
                + " java.lang.annotation.ElementType.LOCAL_VARIABLE,"
                + " java.lang.annotation.ElementType.ANNOTATION_TYPE,"
                + " java.lang.annotation.ElementType.PACKAGE,"
                + " java.lang.annotation.ElementType.TYPE_PARAMETER})");
        copy.addAnnotation(
            "@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE_USE)");

        addAlternate(copy);
      }
    }
  }

  public void extend(String targetTypeName, String extendsName) {
    for (UnsolvedClassOrInterface alternate : getAlternates()) {
      alternate.extend(targetTypeName, extendsName);
    }

    // Special case: if the type to extend is "Annotation", then change the
    // target class to an @interface declaration.
    if ("Annotation".equals(extendsName) || "java.lang.annotation.Annotation".equals(extendsName)) {
      setIsAnAnnotationToTrue();
    }
  }

  public void setNumberOfTypeVariables(int number) {
    for (UnsolvedClassOrInterface alternate : getAlternates()) {
      alternate.setNumberOfTypeVariables(number);
    }
  }

  public String getTypeVariablesAsStringWithoutBrackets() {
    return getAlternates().get(0).getTypeVariablesAsStringWithoutBrackets();
  }
}
