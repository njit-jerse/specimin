package org.checkerframework.specimin.unsolved;

import java.util.List;
import java.util.Set;

/**
 * Class to represent the type of an unsolved field, the return type of an unsolved method, or the
 * types of an unsolved method's parameters.
 *
 * <p>Use this class instead of hardcoding a string into {@link UnsolvedMethod} or {@link
 * UnsolvedField} to ensure proper types when alternates are generated.
 */
public abstract class MemberType {
  private List<MemberType> typeArguments;

  /**
   * Creates a new MemberType with the given type arguments.
   *
   * @param typeArguments The type arguments for this MemberType, which can be empty if there are
   *     none.
   */
  public MemberType(List<MemberType> typeArguments) {
    this.typeArguments = typeArguments;
  }

  /**
   * Gets the set of fully qualified names for this type.
   *
   * @return The set of fully qualified names representing this type
   */
  public abstract Set<String> getFullyQualifiedNames();

  /**
   * Gets the type arguments for this type.
   *
   * @return The list of member types representing the type arguments of this type
   */
  public List<MemberType> getTypeArguments() {
    return typeArguments;
  }

  /**
   * Sets the type arguments for this type.
   *
   * @param typeArguments The list of member types representing the type arguments of this type
   */
  public void setTypeArguments(List<MemberType> typeArguments) {
    this.typeArguments = typeArguments;
  }
}
