package org.checkerframework.specimin.unsolved;

import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a {@link MemberType} of an unsolved symbol, wrapping around an {@link
 * UnsolvedClassOrInterfaceAlternates}.
 *
 * <p>See {@link MemberType} for more details.
 */
public class UnsolvedMemberType implements MemberType {

  private final UnsolvedClassOrInterfaceAlternates unsolved;
  private int numArrayBrackets;

  /**
   * Creates a new UnsolvedMemberType with the given unsolved type and no array brackets.
   *
   * @param unsolved The unsolved type
   */
  public UnsolvedMemberType(UnsolvedClassOrInterfaceAlternates unsolved) {
    this(unsolved, 0);
  }

  /**
   * Creates a new UnsolvedMemberType with the given unsolved type and number of array brackets.
   *
   * @param unsolved The unsolved type
   * @param numArrayBrackets The number of array brackets
   */
  public UnsolvedMemberType(UnsolvedClassOrInterfaceAlternates unsolved, int numArrayBrackets) {
    this.unsolved = unsolved;
    this.numArrayBrackets = numArrayBrackets;
  }

  /**
   * Gets the unsolved type that this member type represents.
   *
   * @return The unsolved type
   */
  public UnsolvedClassOrInterfaceAlternates getUnsolvedType() {
    return unsolved;
  }

  @Override
  public Set<String> getFullyQualifiedNames() {
    return unsolved.getFullyQualifiedNames();
  }

  @Override
  public String toString() {
    // TODO: handle more than one alternate
    return getFullyQualifiedNames().iterator().next() + "[]".repeat(numArrayBrackets);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof UnsolvedMemberType otherAsUnsolvedMemberType)) {
      return false;
    }

    return Objects.equals(otherAsUnsolvedMemberType.unsolved, this.unsolved);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(unsolved);
  }
}
