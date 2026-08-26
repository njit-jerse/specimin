package org.checkerframework.specimin.unsolved;

import java.util.List;

/**
 * Common interface for {@link UnsolvedCallable} and {@link UnsolvedCallableAlternates}. Each getter
 * should return the same value for each alternate; each setter should do the same operation to each
 * alternate. If these requirements are not met, do not include the method in this interface.
 *
 * <p>A method's name and return type are not here: a constructor has neither (JLS 8.8.1), so they
 * belong to {@link UnsolvedMethod} and {@link UnsolvedMethodAlternates}.
 */
public interface UnsolvedCallableCommon {
  /**
   * Getter for the throws list.
   *
   * @return the throws list
   */
  List<MemberType> getThrownExceptions();

  /**
   * Adds an exception to the throws clause of this method, if it is not already present.
   *
   * @param exception the exception to add
   */
  void addThrownException(MemberType exception);

  /**
   * Gets the access modifier (i.e., public, private)
   *
   * @return the access modifier
   */
  String getAccessModifier();

  /** Makes this method static. */
  void setStatic();

  /**
   * Returns true if this method is static
   *
   * @return True if the method is static
   */
  boolean isStatic();

  /**
   * Gets the number of type variables.
   *
   * @return The number of type variables
   */
  int getNumberOfTypeVariables();

  /**
   * Sets the number of type variables.
   *
   * @param number The number of type variables
   */
  void setNumberOfTypeVariables(int number);

  /**
   * Given the index of a type variable, return the name of that type variable.
   *
   * @param index The index
   * @return the name of the type variable with the given index
   */
  String getTypeVariableName(int index);

  /**
   * Declares additional type variables on this method under the given names, appending them after
   * the type variables it already has. Names that this method already declares are skipped.
   *
   * <p>Use this when a name is already written into the signature and merely needs to be bound --
   * notably a type variable of the calling context, which a synthetic member's parameter and return
   * types can mention but which is not in scope where that member is declared. Binding the name
   * that is already there, rather than rewriting the signature to use a generated name, keeps the
   * method's fully-qualified names (which are built from its parameter types' erased simple names)
   * stable.
   *
   * @param names the type variable names to declare
   */
  void declareTypeVariables(List<String> names);

  /**
   * Sets the return type. A constructor has no return type (JLS 8.8.1) and throws from this; see
   * {@link UnsolvedConstructor#setReturnType}.
   *
   * @param memberType The return type
   */
  void setReturnType(MemberType memberType);

  /**
   * Sets the access modifier (i.e., public, private)
   *
   * @param accessModifier The access modifier
   */
  void setAccessModifier(String accessModifier);

  /**
   * Sets the content of the method (default: {@code throw new java.lang.Error();})
   *
   * @param content The content to set to
   */
  void setContent(String content);
}
