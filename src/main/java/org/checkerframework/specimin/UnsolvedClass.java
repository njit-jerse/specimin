package org.checkerframework.specimin;

import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;

/**
 * An UnsolvedClass instance is a representation of a class that can not be solved by SymbolSolver.
 * The reason is that the class file is not in the root directory.
 */
public class UnsolvedClass {
  /** Set of methods belongs to the class */
  private final Set<UnsolvedMethod> methods;

  /** The name of the class */
  private final @ClassGetSimpleName String className;

  /** The fields of this class */
  private final Set<String> classFields;

  /**
   * The name of the package of the class. We rely on the import statements from the source codes to
   * guess the package name.
   */
  private final String packageName;

  /**
   * Create an instance of UnsolvedClass
   *
   * @param className the name of the class
   * @param packageName the name of the package
   */
  public UnsolvedClass(@ClassGetSimpleName String className, String packageName) {
    this.className = className;
    this.methods = new HashSet<>();
    this.packageName = packageName;
    this.classFields = new HashSet<>();
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
   * Get the name of this class.
   *
   * @return the name of the class
   */
  public @ClassGetSimpleName String getClassName() {
    return className;
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
   * Update the return type of a method. Note: this method is supposed to be used to update
   * synthetic methods, where the return type of each method is distinct.
   *
   * @param currentReturnType
   * @param desiredReturnType
   */
  public void updateMethodByReturnType(
      @ClassGetSimpleName String currentReturnType, @ClassGetSimpleName String desiredReturnType) {
    for (UnsolvedMethod method : methods) {
      if (method.getReturnType().equals(currentReturnType)) {
        method.setReturnType(desiredReturnType);
      }
    }
  }

  /**
   * This method updates the types of fields in this class
   *
   * @param currentType the current type
   * @param correctType the desired type
   */
  public void updateFieldByType(String currentType, String correctType) {
    for (String fieldExpression : classFields) {
      String[] elements = fieldExpression.split(" ");
      String fieldType = elements[0];
      String fieldName = elements[1];
      if (fieldType.equals(currentType)) {
        classFields.remove(fieldExpression);
        classFields.add(
            UnsolvedSymbolVisitor.setInitialValueForVariableDeclaration(
                correctType, correctType + " " + fieldName));
      }
    }
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
    sb.append("public class ").append(className).append(" {\n");
    for (String variableDeclarations : classFields) {
      sb.append("    " + "public " + variableDeclarations + ";\n");
    }
    for (UnsolvedMethod method : methods) {
      sb.append(method.toString());
    }
    sb.append("}\n");
    return sb.toString();
  }
}
