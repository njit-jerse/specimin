package org.checkerframework.specimin.unsolved;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An UnsolvedMethod instance is a representation of a method that can not be solved by
 * SymbolSolver. The reason is that the class file of that method is not in the root directory.
 */
public class UnsolvedMethod {

  /** The close() method from java.lang.AutoCloseable. */
  public static final UnsolvedMethod CLOSE =
      new UnsolvedMethod(
          "close",
          MemberType.of("void"),
          Collections.emptyList(),
          List.of(MemberType.of("java.lang.Exception")));

  /** The name of the method */
  private final String name;

  /** The return type of the method. */
  private MemberType returnType;

  /** The list of the types of the parameters of the method. */
  private final List<MemberType> parameterList;

  /** This field is set to true if this method is a static method */
  private boolean isStatic = false;

  /** The list of the types of the exceptions thrown by the method. */
  private final List<MemberType> throwsList;

  /** This field records the number of type variables for this class */
  private int numberOfTypeVariables = 0;

  /**
   * Create an instance of UnsolvedMethod
   *
   * @param name the name of the method
   * @param returnType the return type of the method
   * @param parameterList the list of parameters for this method
   */
  public UnsolvedMethod(String name, MemberType returnType, List<MemberType> parameterList) {
    this(name, returnType, parameterList, List.of());
  }

  /**
   * Create an instance of UnsolvedMethod for a synthetic interface.
   *
   * @param name the name of the method
   * @param returnType the return type of the method
   * @param parameterList the list of parameters for this method
   * @param throwsList the list of exceptions thrown by this method
   */
  public UnsolvedMethod(
      String name,
      MemberType returnType,
      List<MemberType> parameterList,
      List<MemberType> throwsList) {
    this.name = name;
    this.returnType = returnType;
    this.parameterList = parameterList;
    this.throwsList = throwsList;
  }

  /**
   * Get the return type of this method
   *
   * @return the value of returnType
   */
  public MemberType getReturnType() {
    return returnType;
  }

  /**
   * Get the name of this method
   *
   * @return the name of this method
   */
  public String getName() {
    return name;
  }

  /**
   * Getter for the parameter list. Note that the list is read-only.
   *
   * @return the parameter list
   */
  public List<MemberType> getParameterList() {
    return Collections.unmodifiableList(parameterList);
  }

  /** Set isStatic to true */
  public void setStatic() {
    isStatic = true;
  }

  /**
   * This method sets the number of type variables for the current class
   *
   * @param numberOfTypeVariables number of type variable in this class.
   */
  public void setNumberOfTypeVariables(int numberOfTypeVariables) {
    this.numberOfTypeVariables = numberOfTypeVariables;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (!(o instanceof UnsolvedMethod)) {
      return false;
    }
    UnsolvedMethod other = (UnsolvedMethod) o;
    // This set of fields is based on the JLS' overloading rules. According to the documentation of
    // Oracle: "You cannot declare more than one method with the same name and the same number and
    // type of arguments, because the compiler cannot tell them apart. The compiler does not
    // consider return type when differentiating methods, so you cannot declare two methods with the
    // same signature even if they have a different return type."
    return other.name.equals(this.name) && other.parameterList.equals(parameterList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, parameterList);
  }

  /**
   * Return the content of the method. Note that the body of the method is stubbed out.
   *
   * @return the content of the method with the body stubbed out
   */
  public String toString(boolean isInterface) {
    StringBuilder arguments = new StringBuilder();
    for (int i = 0; i < parameterList.size(); i++) {
      MemberType parameter = parameterList.get(i);
      arguments.append(parameter).append(" ").append("parameter").append(i);
      if (i < parameterList.size() - 1) {
        arguments.append(", ");
      }
      throw new RuntimeException("didn't implement; please also see below at throws");
    }
    StringBuilder signature = new StringBuilder();
    signature.append("public ");
    if (isStatic) {
      signature.append("static ");
    }

    String typeVariables = getTypeVariablesAsString();

    if (!typeVariables.equals("")) {
      signature.append(getTypeVariablesAsString()).append(" ");
    }

    if (returnType.isUnsolved() || !"".equals(returnType.getSolvedType())) {
      signature.append(returnType).append(" ");
    }
    signature.append(name).append("(");
    signature.append(arguments);
    signature.append(")");

    if (throwsList.size() > 0) {
      signature.append(" throws ");
    }

    StringBuilder exceptions = new StringBuilder();
    for (int i = 0; i < throwsList.size(); i++) {
      MemberType exception = throwsList.get(i);
      exceptions.append(exception);
      if (i < parameterList.size() - 1) {
        arguments.append(", ");
      }
    }
    signature.append(exceptions);

    if (isInterface) {
      return signature.append(";").toString();
    } else {
      return "\n    " + signature + " {\n        throw new java.lang.Error();\n    }\n";
    }
  }

  /**
   * Return a synthetic representation for type variables of the current class.
   *
   * @return the synthetic representation for type variables
   */
  private String getTypeVariablesAsString() {
    if (numberOfTypeVariables == 0) {
      return "";
    }
    StringBuilder result = new StringBuilder();
    // if class A has three type variables, the expression will be A<T, T1, T2>
    result.append("<");
    getTypeVariablesImpl(result);
    result.append(">");
    return result.toString();
  }

  /**
   * Helper method for {@link #getTypeVariablesAsStringWithoutBrackets} and {@link
   * #getTypeVariablesAsString()}.
   *
   * @param result a string builder. Will be side-effected.
   */
  private void getTypeVariablesImpl(StringBuilder result) {
    for (int i = 0; i < numberOfTypeVariables; i++) {
      String typeExpression = "T" + ((i > 0) ? i : "");
      result.append(typeExpression).append(", ");
    }
    result.delete(result.length() - 2, result.length());
  }
}
