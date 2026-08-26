package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.Node;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;

/**
 * A synthetic constructor declaration. A constructor has no state of its own beyond what every
 * {@link UnsolvedCallable} has: JLS 8.8.1 gives it no return type, and requires it to be declared
 * under the simple name of the type that declares it, so neither is stored here.
 *
 * <p>Two constructors of the same type are the same constructor when their parameters agree, which
 * is why {@link #equals} looks at nothing else.
 */
public class UnsolvedConstructor extends UnsolvedCallable {
  /**
   * Create an instance of UnsolvedConstructor.
   *
   * @param parameterList the list of parameters for this constructor
   * @param throwsList the list of exceptions thrown by this constructor
   * @param mustPreserve the set of nodes that must be preserved with this alternate
   * @param accessModifier the access modifier of this constructor
   * @param typeVariableNames the names of this constructor's type variables, in declaration order
   */
  public UnsolvedConstructor(
      List<MemberType> parameterList,
      List<MemberType> throwsList,
      Set<Node> mustPreserve,
      String accessModifier,
      List<String> typeVariableNames) {
    super(parameterList, throwsList, mustPreserve, accessModifier, typeVariableNames);
  }

  /**
   * A constructor declares no return type (JLS 8.8.1), so there is none to return. Reaching this is
   * a sign that a caller meant to handle methods only; test for {@link UnsolvedMethod} instead.
   *
   * @return never returns
   * @throws UnsupportedOperationException always
   */
  @Override
  public MemberType getReturnType() {
    throw new UnsupportedOperationException("A constructor has no return type: " + this);
  }

  /**
   * A constructor declares no return type (JLS 8.8.1), so there is none to set. Reaching this is a
   * sign that a caller meant to handle methods only; test for {@link UnsolvedMethod} instead.
   *
   * @param returnType ignored
   * @throws UnsupportedOperationException always
   */
  @Override
  public void setReturnType(MemberType returnType) {
    throw new UnsupportedOperationException("A constructor has no return type: " + this);
  }

  @Override
  protected String declaredName(@ClassGetSimpleName String declaringTypeName) {
    return declaringTypeName;
  }

  @Override
  protected String returnTypePrefix() {
    return "";
  }

  @Override
  protected String staticModifier() {
    // A constructor is never static (JLS 8.8).
    return "";
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return o instanceof UnsolvedConstructor other
        && other.getParameterList().equals(getParameterList());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getParameterList());
  }

  @Override
  public String toString() {
    return "constructor(" + getParameterList() + ")";
  }
}
