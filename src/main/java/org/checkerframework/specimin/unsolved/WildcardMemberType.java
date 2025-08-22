package org.checkerframework.specimin.unsolved;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Represents a wildcard type (i.e., ?, ? extends T, or ? super T). */
public class WildcardMemberType extends MemberType {
  /** Represents the type for an unbounded wildcard: ? */
  public static final WildcardMemberType UNBOUNDED = new WildcardMemberType(null, false);

  /** The bound of the wildcard, or null if unbounded. */
  private final @Nullable MemberType bound;

  /**
   * If bound is not null, this indicates whether the wildcard is an upper bound (? extends) or a
   * lower bound (? super).
   */
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

  /**
   * Is the bound an upper bound (? extends) or lower bound (? super)?
   *
   * @return True if this is an upper bound wildcard, false if it is a lower bound.
   */
  public boolean isUpperBounded() {
    return isUpperBound;
  }

  @Override
  public Set<String> getFullyQualifiedNames() {
    Set<String> fqnSet = new LinkedHashSet<>();

    if (bound == null) {
      return Set.of("?");
    }

    String boundString = getBoundString();
    for (String fqn : bound.getFullyQualifiedNames()) {
      fqnSet.add(boundString + fqn);
    }
    return fqnSet;
  }

  /**
   * Gets the string representation of the wildcard's bound, including the wildcard symbol and the
   * appropriate keyword ("extends" or "super") if the bound is not null.
   *
   * @return The string representation of the wildcard's bound.
   */
  private String getBoundString() {
    if (bound == null) {
      return "?";
    }
    return isUpperBound ? "? extends " : "? super ";
  }

  @Override
  public String toString() {
    if (bound == null) {
      return "?";
    }

    return getBoundString() + bound.toString();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof WildcardMemberType otherAsWildcard)) {
      return false;
    }

    return Objects.equals(otherAsWildcard.bound, this.bound)
        && Objects.equals(otherAsWildcard.getBoundString(), this.getBoundString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(bound, getBoundString());
  }

  @Override
  public MemberType copyWithNewTypeArgs(List<MemberType> newTypeArgs) {
    if (newTypeArgs.isEmpty()) {
      return new WildcardMemberType(bound, isUpperBound);
    } else {
      throw new RuntimeException("WildcardMemberType cannot have type arguments");
    }
  }
}
