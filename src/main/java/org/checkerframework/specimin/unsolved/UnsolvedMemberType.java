package org.checkerframework.specimin.unsolved;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a {@link MemberType} of an unsolved symbol, wrapping around an {@link
 * UnsolvedClassOrInterfaceAlternates}.
 *
 * <p>See {@link MemberType} for more details.
 */
public class UnsolvedMemberType extends MemberType {

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
    this(unsolved, numArrayBrackets, List.of());
  }

  /**
   * Creates a new UnsolvedMemberType with the given unsolved type, number of array brackets, and
   * type arguments.
   *
   * @param unsolved The unsolved type
   * @param numArrayBrackets The number of array brackets
   * @param typeArguments The type arguments for this type
   */
  public UnsolvedMemberType(
      UnsolvedClassOrInterfaceAlternates unsolved,
      int numArrayBrackets,
      List<MemberType> typeArguments) {
    super(typeArguments);
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
    StringBuilder sb = new StringBuilder();

    sb.append(getFullyQualifiedNames().iterator().next());

    if (!getTypeArguments().isEmpty()) {
      sb.append('<');
      for (int i = 0; i < getTypeArguments().size(); i++) {
        if (i > 0) {
          sb.append(", ");
        }
        sb.append(getTypeArguments().get(i).toString());
      }
      sb.append('>');
    }

    if (numArrayBrackets > 0) {
      sb.append("[]".repeat(numArrayBrackets));
    }

    return sb.toString();
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
