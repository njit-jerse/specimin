package org.checkerframework.specimin;

import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An UnsolvedMethod instance is a representation of a method that can not be solved by
 * SymbolSolver. The reason is that the class file of that method is not in the root directory.
 */
public class UnsolvedMethod {
  /** The name of the method */
  private final String name;

  /**
   * The return type of the method. At the moment, we set the return type the same as the class
   * where the method belongs to.
   */
  private String returnType;

  /**
   * The list of the types of the parameters of the method. (Right now we won't touch it until the
   * new variant of SymbolSolver is available)
   */
  private List<String> parameterList;

  /** This field is set to true if this method is a static method */
  private boolean isStatic = false;

  /**
   * Indicates whether this instance of UnsolvedMethod represents just a method signature without a
   * body.
   */
  private final boolean isJustMethodSignature;

  /** Access modifer of the current method. The value is set to "public" by default. */
  private final String accessModifier;

  /**
   * Create an instance of UnsolvedMethod
   *
   * @param name the name of the method
   * @param returnType the return type of the method
   * @param parameterList the list of parameters for this method
   */
  public UnsolvedMethod(String name, String returnType, List<String> parameterList) {
    this(name, returnType, parameterList, false);
  }

  /**
   * Create an instance of UnsolvedMethod for a synthetic interface.
   *
   * @param name the name of the method
   * @param returnType the return type of the method
   * @param parameterList the list of parameters for this method
   * @param isJustMethodSignature indicates whether this method represents just a method signature
   *     without a body
   */
  public UnsolvedMethod(
      String name, String returnType, List<String> parameterList, boolean isJustMethodSignature) {
    this(name, returnType, parameterList, isJustMethodSignature, "public");
  }

  /**
   * Create an instance of UnsolvedMethod for a synthetic interface.
   *
   * @param name the name of the method
   * @param returnType the return type of the method
   * @param parameterList the list of parameters for this method
   * @param isJustMethodSignature indicates whether this method represents just a method signature
   *     without a body
   * @param accessModifier the access modifier of the current method
   */
  public UnsolvedMethod(
      String name,
      String returnType,
      List<String> parameterList,
      boolean isJustMethodSignature,
      String accessModifier) {
    this.name = name;
    this.returnType = returnType;
    this.parameterList = parameterList;
    this.isJustMethodSignature = isJustMethodSignature;
    this.accessModifier = accessModifier;
  }

  /**
   * Set the value of returnType. This method is used when javac tells us that UnsolvedSymbolVisitor
   * get the return types wrong.
   *
   * @param returnType the return type to bet set for this method
   */
  public void setReturnType(String returnType) {
    this.returnType = returnType;
  }

  /**
   * Get the return type of this method
   *
   * @return the value of returnType
   */
  public String getReturnType() {
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

  /** Set isStatic to true */
  public void setStatic() {
    isStatic = true;
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
    return Objects.hash(returnType, name, parameterList);
  }

  /**
   * Return the content of the method. Note that the body of the method is stubbed out.
   *
   * @return the content of the method with the body stubbed out
   */
  @Override
  public String toString() {
    StringBuilder arguments = new StringBuilder();
    for (int i = 0; i < parameterList.size(); i++) {
      String parameter = parameterList.get(i);
      arguments.append(parameter).append(" ").append("parameter").append(i);
      if (i < parameterList.size() - 1) {
        arguments.append(", ");
      }
    }
    StringBuilder signature = new StringBuilder();
    signature.append(accessModifier).append(" ");
    if (isStatic) {
      signature.append("static ");
    }
    if (!"".equals(returnType)) {
      signature.append(returnType).append(" ");
    }
    signature.append(name).append("(");
    signature.append(arguments);
    signature.append(")");
    if (isJustMethodSignature) {
      return signature.append(";").toString();
    } else {
      return "\n    " + signature + " {\n        throw new Error();\n    }\n";
    }
  }
}
