package org.checkerframework.specimin;

import java.util.HashSet;
import java.util.Set;

/**
 * An UnsolvedClass instance is a representation of a class that can not be solved by SymbolSolver.
 * The reason is that the class file is not in the root directory.
 */
public class UnsolvedClass {
  /** Set of methods belongs to the class */
  private final Set<UnsolvedMethod> methods;

  /** The name of the class */
  private final String className;

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
  public UnsolvedClass(String className, String packageName) {
    this.className = className;
    this.methods = new HashSet<>();
    this.packageName = packageName;
    // It is simpler to just add the method here than to figure out that it is needed and add it
    // later during the visitor's phase. And if it turns out that the method is not used,
    // MethodPrunerVisitor will take care of it
    UnsolvedMethod constructorMethod = new UnsolvedMethod(className, "");
    methods.add(constructorMethod);
  }

  public Set<UnsolvedMethod> getMethods() {
    return methods;
  }

  public String getClassName() {
    return className;
  }

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
   * Return the content of the class as a compilable Java file.
   *
   * @return the content of the class
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("package ").append(packageName).append(";\n");
    sb.append("class ").append(className).append(" {\n");
    for (UnsolvedMethod method : methods) {
      sb.append(method.toString());
    }
    sb.append("}\n");
    return sb.toString();
  }
}
