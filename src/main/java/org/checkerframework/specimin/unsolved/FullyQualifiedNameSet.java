package org.checkerframework.specimin.unsolved;

import java.util.Collections;
import java.util.List;
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
 */
public record FullyQualifiedNameSet(
    Set<String> erasedFqns, List<FullyQualifiedNameSet> typeArguments, @Nullable String wildcard) {

  /** Represents an unbounded wildcard: ? */
  public static final FullyQualifiedNameSet UNBOUNDED_WILDCARD =
      new FullyQualifiedNameSet(Set.of(), List.of(), "?");

  /**
   * Creates a FullyQualifiedNameSet with erased FQNs, type arguments, but no wildcard.
   *
   * @param erasedFqns A set of erased fully qualified names.
   * @param typeArguments A list of type arguments
   */
  public FullyQualifiedNameSet(Set<String> erasedFqns, List<FullyQualifiedNameSet> typeArguments) {
    this(erasedFqns, typeArguments, null);
  }

  /**
   * Creates a FullyQualifiedNameSet with erased FQNs and no type arguments.
   *
   * @param erasedFqns A set of erased fully qualified names.
   */
  public FullyQualifiedNameSet(Set<String> erasedFqns) {
    this(erasedFqns, Collections.emptyList(), null);
  }

  /**
   * Creates a FullyQualifiedNameSet with erased FQNs and no type arguments.
   *
   * @param erasedFqns A varargs of erased fully qualified names.
   */
  public FullyQualifiedNameSet(String... erasedFqns) {
    this(Set.of(erasedFqns));
  }
}
