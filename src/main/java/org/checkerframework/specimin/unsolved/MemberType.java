package org.checkerframework.specimin.unsolved;

/**
 * Class to represent the type of an unsolved field, the return type of an unsolved method, or the
 * types of an unsolved method's parameters.
 *
 * <p>Use this class instead of hardcoding a string into {@code UnsolvedMethod} or {@code
 * UnsolvedField} to ensure proper types when alternates are generated.
 */
public class MemberType {
  private UnsolvedSymbolAlternates<?> unsolvedType;
  private String solvedType;

  private MemberType() {}

  public static MemberType of(UnsolvedSymbolAlternates<?> symbol) {
    MemberType instance = new MemberType();
    instance.unsolvedType = symbol;
    return instance;
  }

  public static MemberType of(String symbol) {
    MemberType instance = new MemberType();
    instance.solvedType = symbol;
    return instance;
  }

  public void setUnsolvedType(UnsolvedSymbolAlternates<?> symbol) {
    unsolvedType = symbol;
    solvedType = null;
  }

  public void setSolvedType(String symbol) {
    solvedType = symbol;
    unsolvedType = null;
  }

  public boolean isUnsolved() {
    return unsolvedType != null;
  }

  public UnsolvedSymbolAlternates<?> getUnsolvedType() {
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
}
