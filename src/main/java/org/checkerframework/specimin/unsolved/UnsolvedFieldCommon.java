package org.checkerframework.specimin.unsolved;

/**
 * Common interface for {@link UnsolvedField} and {@link UnsolvedFieldAlternates}. Each getter
 * should return the same value for each alternate; each setter should do the same operation to each
 * alternate. If these requirements are not met, do not include the method in this interface.
 */
public interface UnsolvedFieldCommon {
  /**
   * Get the name of this field
   *
   * @return the name of this field
   */
  public String getName();

  /**
   * Check if this field is static
   *
   * @return true if this field is static, false otherwise
   */
  public boolean isStatic();

  /**
   * Check if this field is final
   *
   * @return true if this field is final, false otherwise
   */
  public boolean isFinal();
}
