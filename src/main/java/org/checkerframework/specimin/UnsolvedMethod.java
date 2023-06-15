package org.checkerframework.specimin;

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
  private final String returnType;

  /**
   * Create an instance of UnsolvedMethod
   *
   * @param name the name of the method
   * @param returnType the return type of the method
   */
  public UnsolvedMethod(String name, String returnType) {
    this.name = name;
    this.returnType = returnType;
  }

  /**
   * Return the content of the method. Note that the body of the method is stubbed out.
   *
   * @return the content of the method with the body stubbed out
   */
  public String toString() {
    return "\n    public " + returnType + " " + name + "() {\n        throw new Error();\n    }\n";
  }
}
