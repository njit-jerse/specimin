package org.checkerframework.specimin.unsolved;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a {@link MemberType} of a solved symbol, wrapping around a single String (FQN). {@link
 * #getFullyQualifiedNames()} always returns a set containing only the FQN.
 *
 * <p>See {@link MemberType} for more details.
 */
public class SolvedMemberType extends MemberType {
  /** Represents java.lang.Exception */
  public static final SolvedMemberType JAVA_LANG_EXCEPTION =
      new SolvedMemberType("java.lang.Exception");

  /** Represents java.lang.Error */
  public static final SolvedMemberType JAVA_LANG_ERROR = new SolvedMemberType("java.lang.Error");

  /** Represents java.lang.Object */
  public static final SolvedMemberType JAVA_LANG_OBJECT = new SolvedMemberType("java.lang.Object");

  private String fqn;

  /**
   * Creates a new SolvedMemberType based on a fully-qualified name. May include array brackets.
   *
   * @param fqn The fully-qualified name
   */
  public SolvedMemberType(String fqn) {
    this(fqn, List.of());
  }

  /**
   * Creates a new SolvedMemberType based on a fully-qualified name. May include array brackets.
   * Also provides a list of type arguments.
   *
   * @param fqn The fully-qualified name
   * @param typeArguments The type arguments for this type
   */
  public SolvedMemberType(String fqn, List<MemberType> typeArguments) {
    super(typeArguments);
    this.fqn = fqn;
  }

  @Override
  public Set<String> getFullyQualifiedNames() {
    return Set.of(fqn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    int arrayBracketsIndex = fqn.indexOf("[]");
    String arrayBrackets;
    if (arrayBracketsIndex != -1) {
      arrayBrackets = fqn.substring(arrayBracketsIndex);
      sb.append(fqn.substring(0, arrayBracketsIndex));
    } else {
      arrayBrackets = "";
      sb.append(fqn);
    }

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

    if (!arrayBrackets.isBlank()) {
      sb.append(arrayBrackets);
    }

    return sb.toString();
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof SolvedMemberType otherAsSolvedMemberType)) {
      return false;
    }

    return Objects.equals(otherAsSolvedMemberType.fqn, this.fqn)
        && Objects.equals(otherAsSolvedMemberType.getTypeArguments(), this.getTypeArguments());
  }

  @Override
  public int hashCode() {
    return Objects.hash(fqn, getTypeArguments());
  }

  @Override
  public MemberType copyWithNewTypeArgs(List<MemberType> newTypeArgs) {
    return new SolvedMemberType(fqn, newTypeArgs);
  }
}
