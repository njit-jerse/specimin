package org.checkerframework.specimin.unsolved;

import java.util.LinkedHashSet;
import java.util.List;
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
  private UnsolvedFieldAlternates(
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes) {
    super(alternateDeclaringTypes);
  }

  public static UnsolvedFieldAlternates create(
      String name,
      MemberType type,
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes,
      boolean isStatic,
      boolean isFinal) {
    if (alternateDeclaringTypes.isEmpty()) {
      throw new RuntimeException("Unsolved field must have at least one potential declaring type.");
    }

    UnsolvedFieldAlternates result = new UnsolvedFieldAlternates(alternateDeclaringTypes);

    UnsolvedField field = new UnsolvedField(name, type, isStatic, isFinal);
    result.addAlternate(field);

    return result;
  }

  @Override
  public Set<String> getFullyQualifiedNames() {
    Set<String> fqns = new LinkedHashSet<>();

    for (UnsolvedClassOrInterfaceAlternates alternate : getAlternateDeclaringTypes()) {
      for (String fqn : alternate.getFullyQualifiedNames()) {
        fqns.add(fqn + "#" + getAlternates().get(0).getName());
      }
    }

    return fqns;
  }
}
