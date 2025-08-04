package org.checkerframework.specimin.unsolved;

import org.checkerframework.checker.signature.qual.ClassGetSimpleName;

/**
 * Common interface for {@link UnsolvedClassOrInterface} and {@link
 * UnsolvedClassOrInterfaceAlternates}. Each getter should return the same value for each alternate;
 * each setter should do the same operation to each alternate. If these requirements are not met, do
 * not include the method in this interface.
 */
public interface UnsolvedClassOrInterfaceCommon {
  /**
   * Returns the type of this type. i.e., is it a class, interface, annotation, or enum?
   *
   * @return The type of this type
   */
  public UnsolvedClassOrInterfaceType getType();

  /**
   * Sets the type of this type.
   *
   * @param type The type to set this type to
   */
  public void setType(UnsolvedClassOrInterfaceType type);

  /**
   * Extends this class based on a MemberType.
   *
   * @param extendsType The type to extend
   */
  public void extend(MemberType extendsType);

  /**
   * Returns true if this class has an extends clause.
   *
   * @return True if this class has an extends clause.
   */
  public boolean hasExtends();

  /**
   * Returns true if this class extends the given extendsType.
   *
   * @param extendsType The type to extend
   * @return True if this type extends the given extendsType.
   */
  public boolean doesExtend(MemberType extendsType);

  /**
   * Adds an annotation to this class.
   *
   * @param annotation a fully-qualified annotation to apply
   */
  public void addAnnotation(String annotation);

  /**
   * Implements this class based on a MemberType.
   *
   * @param interfaceName The type to implement
   */
  public void implement(String interfaceName);

  /**
   * Returns true if this class implements the given interface.
   *
   * @param interfaceName The type to implement
   * @return True if this type implements the given interface.
   */
  public boolean doesImplement(String interfaceName);

  /**
   * Sets the number of type variables.
   *
   * @param number The number of type variables.
   */
  public void setNumberOfTypeVariables(int number);

  /**
   * Gets the type variables as a String without brackets (i.e., <T1, T2> --> T1, T2)
   *
   * @return The type variables without brackets
   */
  public String getTypeVariablesAsStringWithoutBrackets();

  /**
   * Return a synthetic representation for type variables of the current class.
   *
   * @return the synthetic representation for type variables
   */
  public String getTypeVariablesAsString();

  /**
   * Gets the number of type variables.
   *
   * @return The number of type variables
   */
  public int getNumberOfTypeVariables();

  /**
   * Get the name of this class (note: without any generic type variables).
   *
   * @return the name of the class
   */
  public @ClassGetSimpleName String getClassName();
}
