package org.checkerframework.specimin.unsolved;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Represents a set of fully qualified names from FullyQualifiedNameGenerator, representing a single
 * type. This record also holds type arguments.
 */
public record FullyQualifiedNameSet(
    Set<String> erasedFqns, List<FullyQualifiedNameSet> typeArguments) {
  /**
   * Creates a FullyQualifiedNameSet with erased FQNs and no type arguments.
   *
   * @param erasedFqns A set of erased fully qualified names.
   */
  public FullyQualifiedNameSet(Set<String> erasedFqns) {
    this(erasedFqns, Collections.emptyList());
  }
}
