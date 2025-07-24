package org.checkerframework.specimin.unsolved;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * /** Given a name, return type, a set of parameters and a set of potential encapsulating classes,
 * this class allows for alternates of a same field to be generated in different locations. If a
 * class were:
 *
 * <pre><code>
 * class A extends B implements C {
 *    void x() {
 *      int y = a();
 *    }
 * }
 * </code></pre>
 *
 * where B and C are both unresolvable, method a() could be in either one. Note that {@code
 * getAlternates()} will always return a single alternate for this class, since {@code
 * UnsolvedMethodAlternates} depends on encapsulating classes for alternate definitions.
 */
public class UnsolvedMethodAlternates extends UnsolvedSymbolAlternates<UnsolvedMethod> {
  private UnsolvedMethodAlternates(
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes) {
    super(alternateDeclaringTypes);
  }

  public static UnsolvedMethodAlternates create(
      String name,
      MemberType type,
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes,
      List<MemberType> parameters) {
    if (alternateDeclaringTypes.isEmpty()) {
      throw new RuntimeException("Unsolved field must have at least one potential declaring type.");
    }
    UnsolvedMethodAlternates result = new UnsolvedMethodAlternates(alternateDeclaringTypes);

    UnsolvedMethod method = new UnsolvedMethod(name, type, parameters);
    result.addAlternate(method);

    return result;
  }

  @Override
  public Set<String> getFullyQualifiedNames() {
    Set<String> fqns = new HashSet<>();

    for (UnsolvedClassOrInterfaceAlternates alternate : getAlternateDeclaringTypes()) {
      for (String fqn : alternate.getFullyQualifiedNames()) {
        fqns.add(fqn + "#" + getAlternates().get(0).getName());
      }
    }

    return fqns;
  }

  public void setIsStaticToTrue() {
    for (UnsolvedMethod method : getAlternates()) {
      method.setStatic();
    }
  }

  public void setNumberOfTypeVariables(int number) {
    for (UnsolvedMethod method : getAlternates()) {
      method.setNumberOfTypeVariables(number);
    }
  }
}
