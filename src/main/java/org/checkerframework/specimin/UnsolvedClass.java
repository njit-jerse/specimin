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
    // It is simpler to just add the method here than to figure out that it is needed and add it
    // later during the visitor's phase. And if it turns out that the method is not used,
    // MethodPrunerVisitor will take care of it
    UnsolvedMethod constructorMethod = new UnsolvedMethod(className, "");
    methods.add(constructorMethod);
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
   * Add a method to the class
   *
   * @param method the method to be added
   */
  public void addMethod(UnsolvedMethod method) {
    this.methods.add(method);
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
   * Return the content of the class as a compilable Java file.
   *
   * @return the content of the class
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("package ").append(packageName).append(";\n");
    sb.append("public class ").append(className).append(" {\n");
    for (UnsolvedMethod method : methods) {
      sb.append(method.toString());
    }
    sb.append("}\n");
    return sb.toString();
  }
}
