package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.Node;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;

/**
 * An UnsolvedMethod instance is a representation of a method that can not be solved by
 * SymbolSolver. The reason is that the class file of that method is not in the root directory. An
 * instance of this class cannot represent a constructor; see {@link UnsolvedConstructor}.
 *
 * <p>Note for {@link #equals}: <strong>Use with caution: two UnsolvedMethods may return not equal
 * but they may belong to the same UnsolvedMethodAlternates. This could be the case when the same
 * unsolved method is called but there are multiple possibilities for a parameter type. When able
 * to, call .equals on UnsolvedMethodAlternates instead of here.</strong>
 */
public class UnsolvedMethod extends UnsolvedCallable {
  /** The name of the method. */
  private final String name;

  /** The return type of the method. */
  private MemberType returnType;

  /** This field is set to true if this method is a static method. */
  private boolean isStatic;

  /**
   * Create an instance of UnsolvedMethod.
   *
   * @param name the name of the method
   * @param returnType the return type of the method
   * @param parameterList the list of parameters for this method
   * @param throwsList the list of exceptions thrown by this method
   * @param mustPreserve the set of nodes that must be preserved with this alternate
   */
  public UnsolvedMethod(
      String name,
      MemberType returnType,
      List<MemberType> parameterList,
      List<MemberType> throwsList,
      Set<Node> mustPreserve) {
    this(name, returnType, parameterList, throwsList, mustPreserve, "public", false, 0);
  }

  /**
   * Create an instance of UnsolvedMethod.
   *
   * @param name the name of the method
   * @param returnType the return type of the method
   * @param parameterList the list of parameters for this method
   * @param throwsList the list of exceptions thrown by this method
   * @param accessModifier the access modifier of this method
   * @param mustPreserve the set of nodes that must be preserved with this alternate
   */
  public UnsolvedMethod(
      String name,
      MemberType returnType,
      List<MemberType> parameterList,
      List<MemberType> throwsList,
      Set<Node> mustPreserve,
      String accessModifier) {
    this(name, returnType, parameterList, throwsList, mustPreserve, accessModifier, false, 0);
  }

  /**
   * Create an instance of UnsolvedMethod.
   *
   * @param name the name of the method
   * @param returnType the return type of the method
   * @param parameterList the list of parameters for this method
   * @param throwsList the list of exceptions thrown by this method
   * @param accessModifier the access modifier of this method
   * @param mustPreserve the set of nodes that must be preserved with this alternate
   * @param isStatic whether this method is static
   * @param numberOfTypeVariables the number of type variables for this method
   */
  public UnsolvedMethod(
      String name,
      MemberType returnType,
      List<MemberType> parameterList,
      List<MemberType> throwsList,
      Set<Node> mustPreserve,
      String accessModifier,
      boolean isStatic,
      int numberOfTypeVariables) {
    this(
        name,
        returnType,
        parameterList,
        throwsList,
        mustPreserve,
        accessModifier,
        isStatic,
        generatedTypeVariableNames(numberOfTypeVariables));
  }

  /**
   * Create an instance of UnsolvedMethod with the given type variable names. Use this, rather than
   * the overload taking a count, when copying an existing method: a name that {@link
   * #declareTypeVariables} bound cannot be reconstructed from a count.
   *
   * @param name the name of the method
   * @param returnType the return type of the method
   * @param parameterList the list of parameters for this method
   * @param throwsList the list of exceptions thrown by this method
   * @param accessModifier the access modifier of this method
   * @param mustPreserve the set of nodes that must be preserved with this alternate
   * @param isStatic whether this method is static
   * @param typeVariableNames the names of this method's type variables, in declaration order
   */
  public UnsolvedMethod(
      String name,
      MemberType returnType,
      List<MemberType> parameterList,
      List<MemberType> throwsList,
      Set<Node> mustPreserve,
      String accessModifier,
      boolean isStatic,
      List<String> typeVariableNames) {
    super(parameterList, throwsList, mustPreserve, accessModifier, typeVariableNames);
    this.name = name;
    this.returnType = returnType;
    this.isStatic = isStatic;
  }

  /**
   * Returns true if this method is static.
   *
   * @return True if the method is static
   */
  public boolean isStatic() {
    return isStatic;
  }

  /** Set isStatic to true */
  public void setStatic() {
    isStatic = true;
  }

  @Override
  protected String staticModifier() {
    return isStatic ? "static " : "";
  }

  @Override
  public MemberType getReturnType() {
    return returnType;
  }

  /**
   * Get the name of this method.
   *
   * @return the name of this method
   */
  public String getName() {
    return name;
  }

  @Override
  protected String declaredName(@ClassGetSimpleName String declaringTypeName) {
    return name;
  }

  @Override
  protected String returnTypePrefix() {
    return returnType + " ";
  }

  @Override
  protected List<MemberType> typesInSignature() {
    List<MemberType> types = new ArrayList<>(getParameterList());
    types.add(returnType);
    return types;
  }

  @Override
  public void setReturnType(MemberType returnType) {
    this.returnType = returnType;
  }

  /**
   * <strong>Use with caution: two UnsolvedMethods may return not equal here but they may belong to
   * the same UnsolvedMethodAlternates. This could be the case when the same unsolved method is
   * called but there are multiple possibilities for a parameter type. When able to, call .equals on
   * UnsolvedMethodAlternates instead of here.</strong>
   *
   * <p>{@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object o) {
    if (!(o instanceof UnsolvedMethod other)) {
      return false;
    }
    return other.name.equals(this.name)
        && other.getParameterList().equals(getParameterList())
        && other.returnType.equals(this.returnType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, getParameterList(), returnType);
  }
}
