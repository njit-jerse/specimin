package org.checkerframework.specimin.unsolved;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.specimin.JavaParserUtil;

/**
 * An UnsolvedMethod instance is a representation of a method that can not be solved by
 * SymbolSolver. The reason is that the class file of that method is not in the root directory.
 *
 * <p>Note for {@link #equals}: <strong>Use with caution: two UnsolvedMethods may return not equal
 * but they may belong to the same UnsolvedMethodAlternates. This could be the case when the same
 * unsolved method is called but there are multiple possibilities for a parameter type. When able
 * to, call .equals on UnsolvedMethodAlternates instead of here.</strong>
 */
public class UnsolvedMethod extends UnsolvedSymbolAlternate implements UnsolvedMethodCommon {
  /** The name of the method */
  private final String name;

  /** The return type of the method. */
  private MemberType returnType;

  /** The list of the types of the parameters of the method. */
  private final List<MemberType> parameterList;

  /** This field is set to true if this method is a static method */
  private boolean isStatic;

  /** The list of the types of the exceptions thrown by the method. */
  private final List<MemberType> throwsList;

  /** The number of type variables for this method. */
  private int numberOfTypeVariables;

  /** The access modifier of the method. */
  private String accessModifier;

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
    super(mustPreserve);
    this.name = name;
    this.returnType = returnType;
    // Parameter list should be mutable, so convert it to be safe
    this.parameterList = new ArrayList<>(parameterList);
    this.throwsList = throwsList;
    this.accessModifier = accessModifier;
    this.isStatic = isStatic;
    this.numberOfTypeVariables = numberOfTypeVariables;
  }

  /**
   * Get the return type of this method.
   *
   * @return the value of returnType
   */
  public MemberType getReturnType() {
    return returnType;
  }

  /**
   * Get the name of this method.
   *
   * @return the name of this method
   */
  @Override
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

  /**
   * Replaces the type of a parameter in the parameter list with a new type.
   *
   * @param oldType The old type
   * @param newType The new type
   */
  public void replaceParameterType(MemberType oldType, MemberType newType) {
    for (int i = 0; i < parameterList.size(); i++) {
      if (parameterList.get(i).equals(oldType)) {
        parameterList.set(i, newType);
      }
    }
  }

  /**
   * Getter for the throws list. Note that the list is read-only.
   *
   * @return the throws list
   */
  @Override
  public List<MemberType> getThrownExceptions() {
    return Collections.unmodifiableList(throwsList);
  }

  /** Set isStatic to true */
  @Override
  public void setStatic() {
    isStatic = true;
  }

  /**
   * This method sets the number of type variables for the current class
   *
   * @param numberOfTypeVariables number of type variable in this class.
   */
  @Override
  public void setNumberOfTypeVariables(int numberOfTypeVariables) {
    this.numberOfTypeVariables = numberOfTypeVariables;
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
        && other.parameterList.equals(parameterList)
        && other.returnType.equals(this.returnType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, parameterList, returnType);
  }

  /**
   * Return the content of the method. Note that the body of the method is stubbed out.
   *
   * @param type The type of the declaring type
   * @return the content of the method with the body stubbed out
   */
  public String toString(UnsolvedClassOrInterfaceType type) {
    StringBuilder arguments = new StringBuilder();
    for (int i = 0; i < parameterList.size(); i++) {
      MemberType parameterType = parameterList.get(i);

      arguments.append(parameterType).append(" ").append("parameter").append(i);
      if (i < parameterList.size() - 1) {
        arguments.append(", ");
      }
    }
    StringBuilder signature = new StringBuilder();
    if (accessModifier != null) {
      signature.append(accessModifier);
      signature.append(" ");
    }

    if (isStatic) {
      signature.append("static ");
    }

    String typeVariables = getTypeVariablesAsString();

    if (!typeVariables.isEmpty()) {
      signature.append(getTypeVariablesAsString()).append(" ");
    }

    String returnTypeAsString = returnType.toString();
    if (!returnTypeAsString.isEmpty()) {
      signature.append(returnTypeAsString).append(" ");
    }
    signature.append(name).append("(");
    signature.append(arguments);
    signature.append(")");

    if (!throwsList.isEmpty()) {
      signature.append(" throws ");
    }

    StringBuilder exceptions = new StringBuilder();
    for (int i = 0; i < throwsList.size(); i++) {
      MemberType exception = throwsList.get(i);
      exceptions.append(exception);
      if (i < throwsList.size() - 1) {
        exceptions.append(", ");
      }
    }
    signature.append(exceptions);

    if (type == UnsolvedClassOrInterfaceType.ANNOTATION
        || type == UnsolvedClassOrInterfaceType.INTERFACE) {
      return "\n    " + signature + ";\n";
    } else {
      return "\n    " + signature + " {\n        throw new java.lang.Error();\n    }\n";
    }
  }

  /**
   * Gets the number of type variables.
   *
   * @return The number of type variables
   */
  @Override
  public int getNumberOfTypeVariables() {
    return numberOfTypeVariables;
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

    return "<" + String.join(", ", getTypeVariablesImpl()) + ">";
  }

  /** Gets a list of the type variable names that are used in this method. */
  private List<String> getTypeVariablesImpl() {
    // While it is better to have the exact type number of type variables
    // on a per-alternate basis, it's easier for other parts of this codebase
    // to deal with the same number of type variables for all alternates of a given method.
    // So, we'll deal with that limitation here and not include any type variables
    // that are not used in this specific alternate.

    // Parse to properly handle type arguments (i.e., a class named TheUnsolved should not be
    // considered to use T)
    List<ClassOrInterfaceType> usedTypes = new ArrayList<>();
    for (MemberType parameterType : parameterList) {
      Type parsedType = StaticJavaParser.parseType(parameterType.toString());

      if (!parsedType.isClassOrInterfaceType()) {
        continue;
      }

      usedTypes.add(parsedType.asClassOrInterfaceType());
    }

    if (returnType != null) {
      Type parsedType = StaticJavaParser.parseType(returnType.toString());

      if (parsedType.isClassOrInterfaceType()) {
        usedTypes.add(parsedType.asClassOrInterfaceType());
      }
    }

    List<String> typeVariableNames = new ArrayList<>();
    for (int i = 0; i < numberOfTypeVariables; i++) {
      String typeVariableName = getTypeVariableName(i);

      for (ClassOrInterfaceType usedType : usedTypes) {
        if (usedType.findAll(ClassOrInterfaceType.class).stream()
            .anyMatch(t -> t.getNameAsString().equals(typeVariableName))) {
          typeVariableNames.add(typeVariableName);
          break;
        }
      }
    }
    return typeVariableNames;
  }

  /**
   * Given the index of a type variable, return the name of that type variable.
   *
   * @param index The index
   * @return the name of the type variable with the given index
   * @throws IllegalArgumentException if the index is out of bounds
   */
  public String getTypeVariableName(int index) {
    if (index < 0 || index >= numberOfTypeVariables) {
      throw new IllegalArgumentException(
          "Index out of bounds. There are only " + numberOfTypeVariables + " type variables.");
    }
    return JavaParserUtil.getGeneratedTypeParameterName(index);
  }

  @Override
  public String getAccessModifier() {
    return accessModifier;
  }

  @Override
  public void setAccessModifier(String accessModifier) {
    this.accessModifier = accessModifier;
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }
}
