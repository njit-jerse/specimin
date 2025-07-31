package org.checkerframework.specimin.unsolved;

import java.util.List;

/**
 * Common interface for {@link UnsolvedMethod} and {@link UnsolvedMethodAlternates}. Each getter
 * should return the same value for each alternate; each setter should do the same operation to each
 * alternate. If these requirements are not met, do not include the method in this interface.
 */
public interface UnsolvedMethodCommon {
  /**
   * Get the name of this method.
   *
   * @return the name of this method
   */
  public String getName();

  /**
   * Getter for the throws list. Note that the list is read-only.
   *
   * @return the throws list
   */
  public List<MemberType> getThrownExceptions();

  /** Makes this method static. */
  public void setStatic();

  /**
   * Gets the number of type variables.
   *
   * @return The number of type variables
   */
  public int getNumberOfTypeVariables();

  /**
   * Sets the number of type variables.
   *
   * @param number The number of type variables
   */
  public void setNumberOfTypeVariables(int number);

  /**
   * Sets the return type.
   *
   * @param memberType The return type
   */
  public void setReturnType(MemberType memberType);
}
