package org.checkerframework.specimin.unsolved;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;

public class UnsolvedClassOrInterface {

  /** The name of the class */
  private final @ClassGetSimpleName String className;

  /**
   * The name of the package of the class. We rely on the import statements from the source codes to
   * guess the package name.
   */
  private final String packageName;

  /** This field records the number of type variables for this class */
  private int numberOfTypeVariables = 0;

  /** This field records the name of type variables that we prefer this class to have. */
  private Set<String> preferredTypeVariables = new HashSet<>();

  /** This field records the extends clause, if one exists. */
  private @Nullable String extendsClause;

  /** The implements clauses, if they exist. */
  private Set<String> implementsClauses = new LinkedHashSet<>(0);

  /** This field records if the class is an interface */
  private boolean isAnInterface;

  /** This class' inner classes. */
  private @MonotonicNonNull Set<UnsolvedClassOrInterface> innerClasses = null;

  /** Is this class an annotation? */
  private boolean isAnAnnotation = false;

  private boolean isInnerClass = false;

  private Set<String> annotations = new HashSet<>();

  /**
   * Create an instance of UnsolvedClass. This constructor correctly splits apart the class name and
   * any generics attached to it.
   *
   * @param className the name of the class, possibly followed by a set of type arguments
   * @param packageName the name of the package
   * @param isInnerClass if the class is an inner class or not
   */
  public UnsolvedClassOrInterface(String className, String packageName, boolean isInnerClass) {
    this(className, packageName, isInnerClass, false);
  }

  /**
   * Create an instance of UnsolvedClass
   *
   * @param className the simple name of the class, possibly followed by a set of type arguments
   * @param packageName the name of the package
   * @param isInnerClass if the class is an inner class or not
   * @param isException does the class represents an exception?
   */
  public UnsolvedClassOrInterface(
      String className, String packageName, boolean isInnerClass, boolean isException) {
    this(className, packageName, isInnerClass, isException, false);
  }

  /**
   * Create an instance of an unsolved interface or unsolved class.
   *
   * @param className the simple name of the interface, possibly followed by a set of type arguments
   * @param packageName the name of the package
   * @param isInnerClass if the class is an inner class or not
   * @param isException does the interface represents an exception?
   * @param isAnInterface check whether this is an interface or a class
   */
  public UnsolvedClassOrInterface(
      String className,
      String packageName,
      boolean isInnerClass,
      boolean isException,
      boolean isAnInterface) {
    if (className.contains("<")) {
      @SuppressWarnings("signature") // removing the <> makes this a true simple name
      @ClassGetSimpleName String classNameWithoutAngleBrackets = className.substring(0, className.indexOf('<'));
      this.className = classNameWithoutAngleBrackets;
    } else {
      @SuppressWarnings("signature") // no angle brackets means this is a true simple name
      @ClassGetSimpleName String classNameWithoutAngleBrackets = className;
      this.className = classNameWithoutAngleBrackets;
    }
    this.packageName = packageName;
    if (isException) {
      this.extendsClause = " extends Exception";
    }
    this.isAnInterface = isAnInterface;
    this.isInnerClass = isInnerClass;
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
   * Returns the value of isAnAnnotation.
   *
   * @return return true if the current UnsolvedClassOrInterface instance represents an annotation.
   */
  public boolean isAnAnnotation() {
    return isAnAnnotation;
  }

  /**
   * Sets isAnInterface to true. isAnInterface is monotonic: it can start as false and become true
   * (because we encounter an implements clause), but it can never go from true to false.
   */
  public void setIsAnInterfaceToTrue() {
    this.isAnInterface = true;
  }

  /**
   * Sets isAnAnnotation to true. isAnAnnotation is monotonic: it can start as false and become true
   * (because we encounter evidence that this is an annotation), but it can never go from true to
   * false.
   */
  public void setIsAnAnnotationToTrue() {
    this.isAnAnnotation = true;
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
  public String getFullyQualifiedName() {
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
   * This method sets the number of type variables for the current class
   *
   * @param numberOfTypeVariables number of type variable in this class.
   */
  public void setNumberOfTypeVariables(int numberOfTypeVariables) {
    this.numberOfTypeVariables = numberOfTypeVariables;
  }

  /**
   * Set the value for preferredTypeVariables.
   *
   * @param preferredTypeVariables desired value for preferredTypeVariables.
   */
  public void setPreferredTypeVariables(Set<String> preferredTypeVariables) {
    this.preferredTypeVariables = preferredTypeVariables;
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
   * Adds a new interface to the list of implemented interfaces.
   *
   * @param interfaceName the fqn of the interface
   */
  public void implement(String interfaceName) {
    implementsClauses.add(interfaceName);
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
   * Adds an annotation to this class.
   *
   * @param className a fully-qualified annotation to apply
   */
  public void addAnnotation(String annotation) {
    this.annotations.add(annotation);
  }

  /**
   * Attempts to add an extends clause to this class or (recursively) to one of its inner classes.
   * An extends clause will only be added if the name of this class matches the target type name.
   * The name of the class in the extends clause is extendsName.
   *
   * @param targetTypeName the name of the class to be extended. This may be a fully-qualified name,
   *     a simple name, or a dot-separated identifier.
   * @param extendsName the name of the class to extend. Always fully-qualified.
   * @return true if an extends clause was added, false otherwise
   */
  public boolean extend(String targetTypeName, String extendsName) {
    if (targetTypeName.equals(this.getFullyQualifiedName())
        || targetTypeName.equals(this.getClassName())) {
      // Special case: if the type to extend is "Annotation", then change the
      // target class to an @interface declaration.
      if (!"Annotation".equals(extendsName)
          && !"java.lang.annotation.Annotation".equals(extendsName)) {
        extend(extendsName);
      }
      return true;
    }
    if (innerClasses == null) {
      return false;
    }
    // Two possibilities, depending on how Javac's error message looks:
    // 1. Javac provides the whole class name in the form Outer.Inner
    // 2. Javac provides only the inner class name
    if (targetTypeName.indexOf('.') != -1) {
      String outerName = targetTypeName.substring(0, targetTypeName.indexOf('.'));
      if (!outerName.equals(this.className)) {
        return false;
      }
      // set the targetTypeName to the name of the inner class
      targetTypeName = targetTypeName.substring(targetTypeName.indexOf('.') + 1);
    }
    boolean result = false;
    for (UnsolvedClassOrInterface unsolvedInnerClass : innerClasses) {
      result |= unsolvedInnerClass.extend(targetTypeName, extendsName);
    }
    return result;
  }

  /**
   * Add the given class as an inner class to this class.
   *
   * @param innerClass the inner class to add
   */
  public void addInnerClass(UnsolvedClassOrInterface innerClass) {
    if (this.innerClasses == null) {
      // LinkedHashSet to make the iteration order deterministic.
      this.innerClasses = new LinkedHashSet<>(1);
    }
    this.innerClasses.add(innerClass);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof UnsolvedClassOrInterface)) {
      return false;
    }
    UnsolvedClassOrInterface otherClass = (UnsolvedClassOrInterface) other;
    // Note: an UnsovledClass cannot represent an anonymous class
    // (each UnsolvedClass corresponds to a source file), so this
    // check is sufficient for equality (it is checking the canonical name).
    return otherClass.className.equals(this.className)
        && otherClass.packageName.equals(this.packageName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(className, packageName);
  }

  public UnsolvedClassOrInterface copy() {
    UnsolvedClassOrInterface copy =
        new UnsolvedClassOrInterface(className, packageName, isInnerClass, false, isAnInterface);

    copy.extendsClause = this.extendsClause;
    copy.implementsClauses = new LinkedHashSet<>(this.implementsClauses);
    copy.innerClasses = new HashSet<>();
    for (UnsolvedClassOrInterface inner : this.innerClasses) {
      copy.innerClasses.add(inner.copy());
    }
    copy.isAnAnnotation = this.isAnAnnotation;
    copy.numberOfTypeVariables = this.numberOfTypeVariables;
    copy.preferredTypeVariables = new HashSet<>(this.preferredTypeVariables);
    copy.annotations = new HashSet<>(this.annotations);

    return copy;
  }

  /**
   * Return the content of the class as a compilable Java file.
   *
   * @return the content of the class
   */
  public String toString(List<UnsolvedMethod> methods, List<UnsolvedField> fields) {
    StringBuilder sb = new StringBuilder();
    if (!this.isInnerClass) {
      sb.append("package ").append(packageName).append(";\n");
    }

    if (!annotations.isEmpty()) {

      sb.append(
          "@java.lang.annotation.Target({ \n"
              + "\tjava.lang.annotation.ElementType.TYPE, \n"
              + "\tjava.lang.annotation.ElementType.FIELD, \n"
              + "\tjava.lang.annotation.ElementType.METHOD, \n"
              + "\tjava.lang.annotation.ElementType.PARAMETER, \n"
              + "\tjava.lang.annotation.ElementType.CONSTRUCTOR, \n"
              + "\tjava.lang.annotation.ElementType.LOCAL_VARIABLE, \n"
              + "\tjava.lang.annotation.ElementType.ANNOTATION_TYPE,\n"
              + "\tjava.lang.annotation.ElementType.PACKAGE,\n"
              + "\tjava.lang.annotation.ElementType.TYPE_PARAMETER,\n"
              + "\tjava.lang.annotation.ElementType.TYPE_USE \n"
              + "})");
    }

    for (String annotation : annotations) {
      sb.append(annotation);
    }

    sb.append("public ");
    if (this.isInnerClass) {
      // Nested classes that are visible outside their parent class
      // are usually static. There is no downside to making them static
      // (it imposes no additional requirements), but there is a downside
      // to making them non-static (they must be attached to a specific member
      // of the outer class, which may or may not be true in the event).
      // TODO: I'm not sure we actually have test cases for "real" inner classes
      // (which are non-static nested classes). All of our "inner class" tests
      // appear to be intended for static nested classes. See
      // https://docs.oracle.com/javase/tutorial/java/javaOO/nested.html for
      // a discussion of the difference.
      sb.append("static ");
    }
    if (isAnInterface) {
      // For synthetic interfaces created for lambdas only.
      if (methods.size() == 1
          && (className.startsWith("SyntheticFunction")
              || className.startsWith("SyntheticConsumer"))) {
        sb.append("@FunctionalInterface\n");
      }
      sb.append("interface ");
    } else if (isAnAnnotation) {
      sb.append("@interface ");
    } else {
      sb.append("class ");
    }
    sb.append(className).append(getTypeVariablesAsString());
    if (extendsClause != null) {
      sb.append(" ").append(extendsClause);
    }
    if (implementsClauses.size() > 0) {
      if (extendsClause != null) {
        sb.append(", ");
      }
      sb.append(" implements ");
      Iterator<String> interfaces = implementsClauses.iterator();
      while (interfaces.hasNext()) {
        sb.append(interfaces.next());
        if (interfaces.hasNext()) {
          sb.append(", ");
        }
      }
    }
    sb.append(" {\n");
    if (innerClasses != null) {
      for (UnsolvedClassOrInterface innerClass : innerClasses) {
        sb.append(innerClass.toString());
      }
    }
    for (UnsolvedField variableDeclarations : fields) {
      sb.append("    ").append(variableDeclarations).append("\n");
    }
    for (UnsolvedMethod method : methods) {
      sb.append(method.toString(isAnInterface));
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
    if (preferredTypeVariables.size() == 0) {
      for (int i = 0; i < numberOfTypeVariables; i++) {
        String typeExpression = "T" + ((i > 0) ? i : "");
        result.append(typeExpression).append(", ");
      }
    } else {
      for (String preferedTypeVar : preferredTypeVariables) {
        result.append(preferedTypeVar).append(", ");
      }
    }
    result.delete(result.length() - 2, result.length());
  }
}
