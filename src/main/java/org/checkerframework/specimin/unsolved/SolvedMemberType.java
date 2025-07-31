package org.checkerframework.specimin.unsolved;

import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a {@link MemberType} of a solved symbol, wrapping around a single String (FQN). {@link
 * #getFullyQualifiedNames()} always returns a set containing only the FQN.
 *
 * <p>See {@link MemberType} for more details.
 */
public class SolvedMemberType implements MemberType {
  private String fqn;

  /**
   * Creates a new SolvedMemberType based on a fully-qualified name. May include array brackets.
   *
   * @param fqn The fully-qualified name
   */
  public SolvedMemberType(String fqn) {
    this.fqn = fqn;
  }

  @Override
  public Set<String> getFullyQualifiedNames() {
    return Set.of(fqn);
  }

  @Override
  public String toString() {
    return fqn;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof SolvedMemberType otherAsSolvedMemberType)) {
      return false;
    }

    return Objects.equals(otherAsSolvedMemberType.fqn, this.fqn);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(fqn);
  }
}
