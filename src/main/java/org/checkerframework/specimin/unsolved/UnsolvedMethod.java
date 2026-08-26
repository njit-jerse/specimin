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
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;
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
  /**
   * The name of the method. For a constructor this is the simple name of the declaring type, which
   * is where {@link #toString} takes it from instead: JLS 8.8.1 fixes a constructor's name to be
   * that simple name, so it is not independent state and no caller supplies it.
   */
  private final String name;

  /** Whether this is a constructor rather than an ordinary method. */
  private final boolean isConstructor;

  /** The return type of the method. */
  private MemberType returnType;

  /** The list of the types of the parameters of the method. */
  private final List<MemberType> parameterList;

  /** This field is set to true if this method is a static method */
  private boolean isStatic;

  /** The list of the types of the exceptions thrown by the method. */
  private final List<MemberType> throwsList;

  /**
   * The names of this method's type variables, in declaration order.
   *
   * <p>These names are the state. Most of a method's type variables are invented by Specimin and
   * named by {@link JavaParserUtil#getGeneratedTypeParameterName}, but one that {@link
   * #declareTypeVariables} binds carries a name that is already written into the signature and
   * cannot be renamed.
   */
  private final List<String> typeVariableNames;

  /** The access modifier of the method. */
  private String accessModifier;

  /** The content of the method. */
  private String content = "throw new java.lang.Error();";

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
    this(
        name,
        returnType,
        parameterList,
        throwsList,
        mustPreserve,
        accessModifier,
        isStatic,
        typeVariableNames,
        false);
  }

  /**
   * Create an instance of UnsolvedMethod, which may be a constructor. Prefer {@link
   * UnsolvedMethodAlternates#createConstructor} to calling this directly with {@code isConstructor}
   * set: it derives the name from the declaring type, which is the only name JLS 8.8.1 permits.
   *
   * @param name the name of the method; for a constructor, the declaring type's simple name
   * @param returnType the return type of the method; the empty type for a constructor
   * @param parameterList the list of parameters for this method
   * @param throwsList the list of exceptions thrown by this method
   * @param accessModifier the access modifier of this method
   * @param mustPreserve the set of nodes that must be preserved with this alternate
   * @param isStatic whether this method is static
   * @param typeVariableNames the names of this method's type variables, in declaration order
   * @param isConstructor whether this is a constructor rather than an ordinary method
   */
  public UnsolvedMethod(
      String name,
      MemberType returnType,
      List<MemberType> parameterList,
      List<MemberType> throwsList,
      Set<Node> mustPreserve,
      String accessModifier,
      boolean isStatic,
      List<String> typeVariableNames,
      boolean isConstructor) {
    super(mustPreserve);
    this.name = name;
    this.isConstructor = isConstructor;
    this.returnType = returnType;

    // Parameter and throws lists should be mutable, so copy them to be safe
    this.parameterList = new ArrayList<>(parameterList);
    this.throwsList = new ArrayList<>(throwsList);

    this.accessModifier = accessModifier;
    this.isStatic = isStatic;
    this.typeVariableNames = new ArrayList<>(typeVariableNames);
  }

  /**
   * Returns the first {@code count} generated type variable names.
   *
   * @param count how many names to generate
   * @return the generated names, in order
   */
  private static List<String> generatedTypeVariableNames(int count) {
    List<String> names = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      names.add(JavaParserUtil.getGeneratedTypeParameterName(i));
    }
    return names;
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

  @Override
  public boolean isConstructor() {
    return isConstructor;
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

  @Override
  public void addThrownException(MemberType exception) {
    if (!throwsList.contains(exception)) {
      throwsList.add(exception);
    }
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
    // Resize rather than replace, so that a name bound by declareTypeVariables at an index below
    // the new size survives.
    while (typeVariableNames.size() > numberOfTypeVariables) {
      typeVariableNames.remove(typeVariableNames.size() - 1);
    }
    while (typeVariableNames.size() < numberOfTypeVariables) {
      typeVariableNames.add(JavaParserUtil.getGeneratedTypeParameterName(typeVariableNames.size()));
    }
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
   * @param declaringTypeName The simple name of the declaring type. A constructor is declared under
   *     this name (JLS 8.8.1), so it is read from the declaring type rather than stored.
   * @return the content of the method with the body stubbed out
   */
  public String toString(
      UnsolvedClassOrInterfaceType type, @ClassGetSimpleName String declaringTypeName) {
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
    signature.append(isConstructor ? declaringTypeName : name).append("(");
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
      return "\n    " + signature + " {\n        " + content + "\n    }\n";
    }
  }

  /**
   * Gets the number of type variables.
   *
   * @return The number of type variables
   */
  @Override
  public int getNumberOfTypeVariables() {
    return typeVariableNames.size();
  }

  /**
   * Returns the names of this method's type variables, in declaration order. Pass this to {@link
   * #UnsolvedMethod(String, MemberType, List, List, Set, String, boolean, List)} when copying.
   *
   * @return the type variable names; the list is read-only
   */
  public List<String> getTypeVariableNames() {
    return Collections.unmodifiableList(typeVariableNames);
  }

  /**
   * Return a synthetic representation for type variables of the current class.
   *
   * @return the synthetic representation for type variables
   */
  private String getTypeVariablesAsString() {
    if (typeVariableNames.isEmpty()) {
      return "";
    }

    List<String> used = getTypeVariablesImpl();

    // A type variable may be allocated for this method but end up used by no alternate's
    // signature, in which case there is no type parameter section to print at all.
    if (used.isEmpty()) {
      return "";
    }

    return "<" + String.join(", ", used) + ">";
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

    // A constructor's return type is the empty string, which is not parseable as a type.
    if (returnType != null && !returnType.toString().isEmpty()) {
      Type parsedType = StaticJavaParser.parseType(returnType.toString());

      if (parsedType.isClassOrInterfaceType()) {
        usedTypes.add(parsedType.asClassOrInterfaceType());
      }
    }

    List<String> usedTypeVariableNames = new ArrayList<>();
    for (String typeVariableName : typeVariableNames) {
      for (ClassOrInterfaceType usedType : usedTypes) {
        if (usedType.findAll(ClassOrInterfaceType.class).stream()
            .anyMatch(t -> t.getNameAsString().equals(typeVariableName))) {
          usedTypeVariableNames.add(typeVariableName);
          break;
        }
      }
    }
    return usedTypeVariableNames;
  }

  /**
   * Given the index of a type variable, return the name of that type variable.
   *
   * @param index The index
   * @return the name of the type variable with the given index
   * @throws IllegalArgumentException if the index is out of bounds
   */
  @Override
  public String getTypeVariableName(int index) {
    if (index < 0 || index >= typeVariableNames.size()) {
      throw new IllegalArgumentException(
          "Index out of bounds. There are only " + typeVariableNames.size() + " type variables.");
    }
    return typeVariableNames.get(index);
  }

  @Override
  public void declareTypeVariables(List<String> names) {
    for (String name : names) {
      if (!typeVariableNames.contains(name)) {
        typeVariableNames.add(name);
      }
    }
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
  public void setContent(String content) {
    this.content = content;
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }
}
