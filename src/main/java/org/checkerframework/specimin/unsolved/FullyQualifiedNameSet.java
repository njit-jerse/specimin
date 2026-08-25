package org.checkerframework.specimin.unsolved;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a set of fully qualified names from FullyQualifiedNameGenerator, representing a single
 * type. This record also holds type arguments and a wildcard if applicable. The parameter for
 * wildcard should hold either "?", "? extends", or "? super".
 *
 * <p>For example, if representing the set {@code [? extends org.example.A<org.example.B>, ? extends
 * com.example.A<com.example.B>]}, then pass in a set of erasedFqns {@code [org.example.A,
 * com.example.A]}, a list of FullyQualifiedNameSet {@code [org.example.B, com.example.B]} for type
 * arguments, and a wildcard of {@code ? extends}.
 *
 * <p>usesGeneratedName represents whether this FullyQualifiedNameSet uses a generated type name
 * (i.e., GetListReturnType).
 */
public record FullyQualifiedNameSet(
    Set<String> erasedFqns,
    List<FullyQualifiedNameSet> typeArguments,
    @Nullable String wildcard,
    boolean usesGeneratedName) {
  /**
   * Creates a new FullyQualifiedNameSet.
   *
   * @param erasedFqns A set of FQNs with no type arguments.
   * @param typeArguments The list of type arguments, or an empty list if none.
   * @param wildcard The wildcard (?, ? extends, ? super) or null if none.
   * @param usesGeneratedName Whether this FQNSet represents an inferred, generated type name versus
   *     a known type name (i.e., FooReturnType)
   */
  public FullyQualifiedNameSet {
    for (String fqn : erasedFqns) {
      if (fqn.contains("?")) {
        throw new IllegalArgumentException(
            "erasedFqns cannot contain a wildcard; use parameter wildcard instead: " + erasedFqns);
      }
    }

    if (wildcard != null
        && !wildcard.equals("?")
        && !wildcard.equals("? extends")
        && !wildcard.equals("? super")) {
      throw new IllegalArgumentException(
          "wildcard must be either ?, ? extends, or ? super: " + wildcard);
    }
  }

  /** Represents an unbounded wildcard: ? */
  public static final FullyQualifiedNameSet UNBOUNDED_WILDCARD =
      new FullyQualifiedNameSet(Set.of(), List.of(), "?");

  /**
   * Creates a non-synthetic FullyQualifiedNameSet with erased FQNs, type arguments, and a wildcard.
   *
   * @param erasedFqns A set of erased fully qualified names.
   * @param typeArguments A list of type arguments.
   * @param wildcard The wildcard for the fully qualified name set.
   */
  public FullyQualifiedNameSet(
      Set<String> erasedFqns,
      List<FullyQualifiedNameSet> typeArguments,
      @Nullable String wildcard) {
    this(erasedFqns, typeArguments, wildcard, false);
  }

  /**
   * Creates a non-synthetic FullyQualifiedNameSet with erased FQNs, type arguments, but no
   * wildcard.
   *
   * @param erasedFqns A set of erased fully qualified names.
   * @param typeArguments A list of type arguments
   */
  public FullyQualifiedNameSet(Set<String> erasedFqns, List<FullyQualifiedNameSet> typeArguments) {
    this(erasedFqns, typeArguments, null);
  }

  /**
   * Creates a non-synthetic FullyQualifiedNameSet with erased FQNs and no type arguments.
   *
   * @param erasedFqns A set of erased fully qualified names.
   */
  public FullyQualifiedNameSet(Set<String> erasedFqns) {
    this(erasedFqns, Collections.emptyList(), null);
  }

  /**
   * Creates a non-synthetic FullyQualifiedNameSet with erased FQNs and no type arguments.
   *
   * @param erasedFqns A varargs of erased fully qualified names.
   */
  public FullyQualifiedNameSet(String... erasedFqns) {
    this(Set.of(erasedFqns));
  }

  /**
   * Returns the one fully-qualified name that the given inferred type unambiguously names, or null
   * if there is not exactly one.
   *
   * <p>An inferred type is a set of FullyQualifiedNameSets, each of which is itself a set of erased
   * FQNs, because Specimin may be unsure both of which type an expression has and of which package
   * that type is in. Callers that must know the exact type -- rather than merely constrain it --
   * can only proceed when both of those sets are singletons, and this method is how they ask.
   *
   * @param inferredType an inferred type, as produced by FullyQualifiedNameGenerator
   * @return the sole erased FQN, or null if the inferred type names zero or several
   */
  public static @Nullable String getSoleErasedFqn(Set<FullyQualifiedNameSet> inferredType) {
    if (inferredType.size() != 1) {
      return null;
    }
    Set<String> erasedFqns = inferredType.iterator().next().erasedFqns();
    return erasedFqns.size() == 1 ? erasedFqns.iterator().next() : null;
  }

  /**
   * Removes the given number of array levels from every name in the given inferred type, or returns
   * null if some name is not an array that deep.
   *
   * <p>JLS 4.10.3 derives array subtyping from the element types: {@code S[]} is a subtype of
   * {@code T[]} exactly when {@code S} is a subtype of {@code T}. A caller that has to relate two
   * array types can use this method to strip the brackets off both before it relates what is left.
   *
   * @param inferredType an inferred type, as produced by FullyQualifiedNameGenerator
   * @param levels how many array levels to remove; must not be negative
   * @return the element type at the given depth, or null if some name is not an array that deep
   */
  public static @Nullable Set<FullyQualifiedNameSet> stripArrayLevels(
      Set<FullyQualifiedNameSet> inferredType, int levels) {
    String brackets = "[]".repeat(levels);
    Set<FullyQualifiedNameSet> result = new LinkedHashSet<>();

    for (FullyQualifiedNameSet fqnSet : inferredType) {
      Set<String> stripped = new LinkedHashSet<>();

      for (String fqn : fqnSet.erasedFqns()) {
        if (!fqn.endsWith(brackets)) {
          return null;
        }
        stripped.add(fqn.substring(0, fqn.length() - brackets.length()));
      }

      result.add(
          new FullyQualifiedNameSet(
              stripped, fqnSet.typeArguments(), fqnSet.wildcard(), fqnSet.usesGeneratedName()));
    }

    return result;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other instanceof FullyQualifiedNameSet otherSet) {
      return Objects.equals(erasedFqns, otherSet.erasedFqns)
          && Objects.equals(typeArguments, otherSet.typeArguments)
          && Objects.equals(wildcard, otherSet.wildcard);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(erasedFqns, typeArguments, wildcard);
  }
}
