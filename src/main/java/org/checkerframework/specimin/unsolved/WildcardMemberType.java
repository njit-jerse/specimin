package org.checkerframework.specimin.unsolved;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Represents a wildcard type (i.e., ?, ? extends T, or ? super T). */
public class WildcardMemberType extends MemberType {

  public static final WildcardMemberType UNBOUNDED = new WildcardMemberType(null, false);

  private final @Nullable MemberType bound;
  private final boolean isUpperBound;

  /**
   * Creates a new WildcardMemberType with an optional bound. If the bound is null, it represents an
   * unbounded wildcard (i.e., "?"). If the bound is not null, use isUpperBound to determine whether
   * this is an upper or lower bound wildcard (i.e., "? extends T" or "? super T").
   *
   * @param bound The bound of the wildcard, or null for an unbounded wildcard.
   * @param isUpperBound True if this is an upper bound wildcard, false if it is a lower bound. If
   *     bound is null, this parameter is ignored.
   */
  public WildcardMemberType(@Nullable MemberType bound, boolean isUpperBound) {
    super(List.of());
    this.bound = bound;
    this.isUpperBound = isUpperBound;
  }

  /**
   * Gets the bound of this wildcard type. If this is an unbounded wildcard, this will return null.
   *
   * @return The bound of the wildcard
   */
  public @Nullable MemberType getBound() {
    return bound;
  }

  @Override
  public Set<String> getFullyQualifiedNames() {
    Set<String> fqnSet = new LinkedHashSet<>();

    if (bound == null) {
      return Set.of("?");
    }

    for (String fqn : bound.getFullyQualifiedNames()) {
      if (isUpperBound) {
        fqnSet.add("? extends " + fqn);
      } else {
        fqnSet.add("? super " + fqn);
      }
    }
    return fqnSet;
  }

  @Override
  public String toString() {
    if (bound == null) {
      return "?";
    }

    if (isUpperBound) {
      return "? extends " + bound.toString();
    } else {
      return "? super " + bound.toString();
    }
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof WildcardMemberType otherAsWildcard)) {
      return false;
    }

    return Objects.equals(otherAsWildcard.bound, this.bound)
        && otherAsWildcard.isUpperBound == this.isUpperBound;
  }

  @Override
  public int hashCode() {
    return Objects.hash(bound, isUpperBound);
  }
}
