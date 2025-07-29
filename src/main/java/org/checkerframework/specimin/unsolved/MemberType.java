package org.checkerframework.specimin.unsolved;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Class to represent the type of an unsolved field, the return type of an unsolved method, or the
 * types of an unsolved method's parameters.
 *
 * <p>Use this class instead of hardcoding a string into {@link UnsolvedMethod} or {@link
 * UnsolvedField} to ensure proper types when alternates are generated.
 *
 * <p>If {@link #unsolvedType} is set, {@link #solvedType} is null; if {@link #solvedType} is set,
 * {@link #unsolvedType} is null.
 */
public class MemberType {
  private @Nullable UnsolvedClassOrInterfaceAlternates unsolvedType;
  private int numArrayBrackets;
  private @Nullable String solvedType;

  /**
   * Creates a new MemberType, given an unsolved type.
   *
   * @param unsolvedType The unsolved type
   */
  public MemberType(UnsolvedClassOrInterfaceAlternates unsolvedType) {
    this(unsolvedType, 0);
  }

  /**
   * Creates a new MemberType, given an unsolved type and the number of array brackets.
   *
   * @param unsolvedType The unsolved type
   */
  public MemberType(UnsolvedClassOrInterfaceAlternates unsolvedType, int numArrayBrackets) {
    this.unsolvedType = unsolvedType;
    this.numArrayBrackets = numArrayBrackets;
    this.solvedType = null;
  }

  /**
   * Creates a new MemberType, given a solved type.
   *
   * @param solvedType The solved type, preferably as a FQN.
   */
  public MemberType(String solvedType) {
    this.unsolvedType = null;
    this.solvedType = solvedType;
  }

  /**
   * Sets this MemberType to the unsolved type, and sets the solved type to null.
   *
   * @param type The unsolved type to set to
   */
  public void setUnsolvedType(UnsolvedClassOrInterfaceAlternates type) {
    unsolvedType = type;
    solvedType = null;
  }

  /**
   * Sets this MemberType to the solved type, and sets the unsolved type to null.
   *
   * @param type The solved type to set to
   */
  public void setSolvedType(String type) {
    solvedType = type;
    unsolvedType = null;
    numArrayBrackets = 0;
  }

  /**
   * Sets the number of [] for this type, if unsolved. For example, com.example.MyFoo[][][] should
   * call setNumArrayBrackets(3) to get the proper number of bracket pairs. If using a solved type,
   * just set the brackets directly into the String passed into setSolvedType.
   *
   * @param num The number of []s
   */
  public void setNumArrayBrackets(int num) {
    numArrayBrackets = num;
  }

  /**
   * Returns whether this type is unsolved.
   *
   * @return True if this type represents an unsolved type.
   */
  public boolean isUnsolved() {
    return unsolvedType != null && solvedType == null;
  }

  /**
   * Gets the unsolved type. Ensure that {@link #isUnsolved()} returns true before calling this
   * method; if not, then this method will throw.
   *
   * @return The unsolved type
   */
  public UnsolvedClassOrInterfaceAlternates getUnsolvedType() {
    if (unsolvedType == null) {
      throw new RuntimeException(
          "Attempting to get unsolved type when type is solved. Use isUnsolved() before to check"
              + " which type you should be looking for.");
    }
    return unsolvedType;
  }

  /**
   * Gets the solved type. Ensure that {@link #isUnsolved()} returns false before calling this
   * method; if not, then this method will throw.
   *
   * @return The solved type
   */
  public String getSolvedType() {
    if (solvedType == null) {
      throw new RuntimeException(
          "Attempting to get solved type when type is unsolved. Use isUnsolved() before to check"
              + " which type you should be looking for.");
    }
    return solvedType;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (other instanceof MemberType type) {
      return Objects.equals(type.solvedType, solvedType)
          && Objects.equals(type.unsolvedType, unsolvedType);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(solvedType, unsolvedType);
  }

  @Override
  public String toString() {
    if (isUnsolved()) {
      // TODO: handle more than one alternate
      return getUnsolvedType().getFullyQualifiedNames().iterator().next()
          + "[]".repeat(numArrayBrackets);
    } else {
      return getSolvedType();
    }
  }
}
