package org.checkerframework.specimin.unsolved;

import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
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
   * Returns true if this class has an extends clause.
   *
   * @return True if this class has an extends clause.
   */
  public boolean hasExtends();

  /**
   * Returns true if any alternate extends the given extendsType.
   *
   * @param extendsType The superclass
   * @return True if any alternate extends the given extendsType.
   */
  public boolean doesExtend(MemberType extendsType);

  /**
   * Adds an annotation to this class.
   *
   * @param annotation a fully-qualified annotation to apply
   */
  public void addAnnotation(String annotation);

  /**
   * Returns true if any alternate implements the given interface.
   *
   * @param interfaceType The type of the interface
   * @return True if this type implements the given interface.
   */
  public boolean doesImplement(MemberType interfaceType);

  /**
   * Sets the number of type variables.
   *
   * @param number The number of type variables.
   */
  public void setNumberOfTypeVariables(int number);

  /**
   * Sets the preferred type variables.
   *
   * @param preferredTypeVariables The preferred type variables.
   */
  public void setPreferredTypeVariables(@Nullable List<String> preferredTypeVariables);

  /**
   * Gets the preferred type variables.
   *
   * @return The preferred type variables.
   */
  public @Nullable List<String> getPreferredTypeVariables();

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
