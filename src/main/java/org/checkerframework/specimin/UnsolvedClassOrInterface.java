package org.checkerframework.specimin;

import com.google.common.base.Splitter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;

/**
 * An UnsolvedClassOrInterface instance is a representation of a class or an interface that can not
 * be solved by SymbolSolver. The reason is that the class file is not in the root directory.
 */
public class UnsolvedClassOrInterface {
  /**
   * Set of methods belongs to the class. Must be a linked set to ensure deterministic iteration
   * order when writing files synthetic classes.
   */
  private final LinkedHashSet<UnsolvedMethod> methods;

  /** The name of the class */
  private final @ClassGetSimpleName String className;

  /**
   * The fields of this class. Must be a linked set to ensure deterministic iteration order when
   * writing files for synthetic classes.
   */
  private final LinkedHashSet<String> classFields;

  /**
   * The name of the package of the class. We rely on the import statements from the source codes to
   * guess the package name.
   */
  private final String packageName;

  /** This field records the number of type variables for this class */
  private int numberOfTypeVariables = 0;

  /** The field records the extends/implements clauses, if one exists. */
  private @Nullable String extendsClause;

  /** This field records if the class is an interface */
  private final boolean isAnInterface;

  /**
   * Create an instance of UnsolvedClass. This constructor correctly splits apart the class name and
   * any generics attached to it.
   *
   * @param className the name of the class, possibly followed by a set of type arguments
   * @param packageName the name of the package
   */
  public UnsolvedClassOrInterface(String className, String packageName) {
    this(className, packageName, false);
  }

  /**
   * Create an instance of UnsolvedClass
   *
   * @param className the simple name of the class, possibly followed by a set of type arguments
   * @param packageName the name of the package
   * @param isException does the class represents an exception?
   */
  public UnsolvedClassOrInterface(String className, String packageName, boolean isException) {
    this(className, packageName, isException, false);
  }

  /**
   * Create an instance of an unsolved interface or unsolved class.
   *
   * @param className the simple name of the interface, possibly followed by a set of type arguments
   * @param packageName the name of the package
   * @param isException does the interface represents an exception?
   * @param isAnInterface check whether this is an interface or a class
   */
  public UnsolvedClassOrInterface(
      String className, String packageName, boolean isException, boolean isAnInterface) {
    if (className.contains("<")) {
      @SuppressWarnings("signature") // removing the <> makes this a true simple name
      @ClassGetSimpleName String classNameWithoutAngleBrackets = className.substring(0, className.indexOf('<'));
      this.className = classNameWithoutAngleBrackets;
    } else {
      @SuppressWarnings("signature") // no angle brackets means this is a true simple name
      @ClassGetSimpleName String classNameWithoutAngleBrackets = className;
      this.className = classNameWithoutAngleBrackets;
    }
    this.methods = new LinkedHashSet<>();
    this.packageName = packageName;
    this.classFields = new LinkedHashSet<>();
    if (isException) {
      this.extendsClause = " extends Exception";
    }
    this.isAnInterface = isAnInterface;
  }

  /**
   * Returns the value of isAnInterface.
   *
   * @return return true if the current UnsolvedClassOrInterface instance represents an interface.
   */
  public boolean isAnInterface() {
    return isAnInterface;
  }

  /**
   * Get the list of methods from this synthetic class
   *
   * @return the list of methods
   */
  public Set<UnsolvedMethod> getMethods() {
    return methods;
  }

  /**
   * Get the name of this class (note: without any generic type variables).
   *
   * @return the name of the class
   */
  public @ClassGetSimpleName String getClassName() {
    return className;
  }

  /**
   * Return the qualified name of this class.
   *
   * @return the qualified name
   */
  public String getQualifiedClassName() {
    return packageName + "." + className;
  }

  /**
   * Get the package where this class belongs to
   *
   * @return the value of packageName
   */
  public String getPackageName() {
    return packageName;
  }

  /**
   * Get the fields of this current class
   *
   * @return classVariables
   */
  public Set<String> getClassFields() {
    return classFields;
  }

  /**
   * Add a method to the class
   *
   * @param method the method to be added
   */
  public void addMethod(UnsolvedMethod method) {
    this.methods.add(method);
  }

  /**
   * Add field declaration to the class. We expect something like "int i" or "String y" instead of
   * just "i" and "y"
   *
   * @param variableExpression the expression of the variables to be added
   */
  public void addFields(String variableExpression) {
    this.classFields.add(variableExpression);
  }

  /**
   * This method sets the number of type variables for the current class
   *
   * @param numberOfTypeVariables number of type variable in this class.
   */
  public void setNumberOfTypeVariables(int numberOfTypeVariables) {
    this.numberOfTypeVariables = numberOfTypeVariables;
  }

  /**
   * This method tells the number of type variables for this class
   *
   * @return the number of type variables
   */
  public int getNumberOfTypeVariables() {
    return this.numberOfTypeVariables;
  }

  /**
   * Adds an extends clause to this class.
   *
   * @param className a fully-qualified class name for the class to be extended
   */
  public void extend(String className) {
    this.extendsClause = "extends " + className;
  }

  /**
   * Update the return type of a method. Note: this method is supposed to be used to update
   * synthetic methods, where the return type of each method is distinct.
   *
   * @param currentReturnType the current return type of this method
   * @param desiredReturnType the new return type
   * @return true if a type is successfully updated
   */
  public boolean updateMethodByReturnType(String currentReturnType, String desiredReturnType) {
    boolean successfullyUpdated = false;
    for (UnsolvedMethod method : methods) {
      if (method.getReturnType().equals(currentReturnType)) {
        method.setReturnType(desiredReturnType);
        successfullyUpdated = true;
      }
    }
    return successfullyUpdated;
  }

  /**
   * This method updates the types of fields in this class
   *
   * @param currentType the current type
   * @param correctType the desired type
   * @return true if a type is successfully updated.
   */
  public boolean updateFieldByType(String currentType, String correctType) {
    boolean successfullyUpdated = false;
    Iterator<String> iterator = classFields.iterator();
    Set<String> newFields = new HashSet<>();
    while (iterator.hasNext()) {
      String fieldDeclared = iterator.next();
      String staticKeyword = "";
      String finalKeyword = "";
      // since these are fields in synthetic classes created by UnsolvedSymbolVisitor, if this field
      // is both static and final, the static keyword will be placed before the final keyword.
      if (fieldDeclared.startsWith("static")) {
        fieldDeclared = fieldDeclared.replace("static ", "");
        staticKeyword = "static ";
      }
      if (fieldDeclared.startsWith("final")) {
        fieldDeclared = fieldDeclared.replace("final ", "");
        finalKeyword = "final ";
      }
      List<String> elements = Splitter.on(' ').splitToList(fieldDeclared);
      // fieldExpression is guaranteed to have the form "TYPE FIELD_NAME". Since this field
      // expression is from a synthetic class, there is no annotation involved, so TYPE has no
      // space.
      String fieldType = elements.get(0);
      String fieldName = elements.get(1);
      // endsWith here is important, because the output of javac (i.e., what it prints in the error
      // message, which turns into currentType) is always a simple name, but fields in superclasses
      // are output using FQNs
      if (fieldType.endsWith(currentType)) {
        successfullyUpdated = true;
        iterator.remove();
        newFields.add(
            UnsolvedSymbolVisitor.setInitialValueForVariableDeclaration(
                correctType, staticKeyword + finalKeyword + correctType + " " + fieldName));
      }
    }

    for (String field : newFields) {
      classFields.add(field);
    }
    return successfullyUpdated;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof UnsolvedClassOrInterface)) {
      return false;
    }
    UnsolvedClassOrInterface otherClass = (UnsolvedClassOrInterface) other;
    // Note: an UnsovledClass cannot represent an anonymous class
    // (each UnsovledClass corresponds to a source file), so this
    // check is sufficient for equality (it is checking the canonical name).
    return otherClass.className.equals(this.className)
        && otherClass.packageName.equals(this.packageName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(className, packageName);
  }

  /**
   * Return the content of the class as a compilable Java file.
   *
   * @return the content of the class
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("package ").append(packageName).append(";\n");
    if (isAnInterface) {
      // For synthetic interfaces created for lambdas only.
      if (methods.size() == 1
          && (className.startsWith("SyntheticFunction")
              || className.startsWith("SyntheticConsumer"))) {
        sb.append("@FunctionalInterface\n");
      }
      sb.append("public interface ").append(className).append(getTypeVariablesAsString());
    } else {
      sb.append("public class ").append(className).append(getTypeVariablesAsString());
    }
    if (extendsClause != null) {
      sb.append(" " + extendsClause);
    }
    sb.append(" {\n");
    for (String variableDeclarations : classFields) {
      sb.append("    " + "public " + variableDeclarations + ";\n");
    }
    for (UnsolvedMethod method : methods) {
      sb.append(method.toString());
    }
    sb.append("}\n");
    return sb.toString();
  }

  /**
   * Return a synthetic representation for type variables of the current class.
   *
   * @return the synthetic representation for type variables
   */
  public String getTypeVariablesAsString() {
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
   * Return a synthetic representation for type variables of the current class, without surrounding
   * angle brackets.
   *
   * @return the synthetic representation for type variables
   */
  public String getTypeVariablesAsStringWithoutBrackets() {
    if (numberOfTypeVariables == 0) {
      return "";
    }
    StringBuilder result = new StringBuilder();
    getTypeVariablesImpl(result);
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
