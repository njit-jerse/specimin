package org.checkerframework.specimin.unsolved;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Class to represent the type of an unsolved field, the return type of an unsolved method, or the
 * types of an unsolved method's parameters.
 *
 * <p>Use this class instead of hardcoding a string into {@link UnsolvedMethod} or {@link
 * UnsolvedField} to ensure proper types when alternates are generated.
 */
public class MemberType {
  private @Nullable UnsolvedClassOrInterfaceAlternates unsolvedType;
  private @Nullable String solvedType;

  public MemberType(UnsolvedClassOrInterfaceAlternates unsolvedType) {
    this.unsolvedType = unsolvedType;
    this.solvedType = null;
  }

  public MemberType(String solvedType) {
    this.unsolvedType = null;
    this.solvedType = solvedType;
  }

  public void setUnsolvedType(UnsolvedClassOrInterfaceAlternates symbol) {
    unsolvedType = symbol;
    solvedType = null;
  }

  public void setSolvedType(String symbol) {
    solvedType = symbol;
    unsolvedType = null;
  }

  public boolean isUnsolved() {
    return unsolvedType != null && solvedType == null;
  }

  public UnsolvedClassOrInterfaceAlternates getUnsolvedType() {
    if (unsolvedType == null) {
      throw new RuntimeException(
          "Attempting to get unsolved type when type is solved. Use isUnsolved() before to check"
              + " which type you should be looking for.");
    }
    return unsolvedType;
  }

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
}
