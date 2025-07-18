package org.checkerframework.specimin.unsolved;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Given a name, field type, and a set of potential encapsulating classes, this class allows for
 * alternates of a same field to be generated in different locations. If a class were:
 *
 * <pre><code>
 * class A extends B implements C {
 *    void x() {
 *      int y = a;
 *    }
 * }
 * </code></pre>
 *
 * where B and C are both unresolvable, field a could be in either one. Note that {@code
 * getAlternates()} will always return a single alternate for this class, since {@code
 * UnsolvedFieldAlternates} depends on encapsulating classes for alternate definitions.
 */
public class UnsolvedFieldAlternates extends UnsolvedSymbolAlternates<UnsolvedField> {
  private UnsolvedFieldAlternates() {}

  private Collection<UnsolvedClassOrInterfaceAlternates> potentialEncapsulations;

  public static UnsolvedFieldAlternates create(
      String name,
      MemberType type,
      Collection<UnsolvedClassOrInterfaceAlternates> potentialEncapsulations,
      boolean isStatic,
      boolean isFinal) {
    UnsolvedFieldAlternates result = new UnsolvedFieldAlternates();

    UnsolvedField field = new UnsolvedField(name, type, isStatic, isFinal);

    result.potentialEncapsulations = potentialEncapsulations;
    result.addAlternate(field);

    return result;
  }

  public Set<String> getFullyQualifiedNames() {
    Set<String> fqns = new HashSet<>();

    for (UnsolvedClassOrInterfaceAlternates alternate : potentialEncapsulations) {
      for (String fqn : alternate.getFullyQualifiedNames()) {
        fqns.add(fqn + "#" + getAlternates().get(0).getName());
      }
    }

    return fqns;
  }
}
