package org.checkerframework.specimin;

import com.google.common.base.Splitter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
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

  /**
   * This class' constructor should be used for creating inner classes. Frankly, this design is a
   * mess (sorry) - controlling whether this is an inner class via inheritance is probably bad.
   * TODO: clean this up after ISSTA.
   */
  public static class UnsolvedInnerClass extends UnsolvedClassOrInterface {
    /**
     * Create an instance of UnsolvedInnerClass.
     *
     * @param className the name of the inner class, possibly followed by a set of type arguments
     * @param packageName the name of the package containing the outer class
     */
    public UnsolvedInnerClass(String className, String packageName) {
      super(className, packageName);
    }
  }

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
    // Check for another method with the same parameter list, but with differences in
    // whether the parameter names are fully-qualified or simple names.
    List<String> paramList = method.getParameterList();
    UnsolvedMethod matchingMethod = null;
    boolean preferOther = true;
    methods:
    for (UnsolvedMethod otherMethod : this.methods) {
      // Skip methods that are definitely different.
      if (!method.getName().equals(otherMethod.getName())
          || otherMethod.getParameterList().size() != paramList.size()) {
        continue;
      }

      for (int i = 0; i < paramList.size(); ++i) {
        String paramType = paramList.get(i);
        String otherParamType = otherMethod.getParameterList().get(i);
        if ((this.packageName + "." + otherParamType).equals(paramType)) {
          // In this case, the current method has the FQNs.
          preferOther = false;
        } else if (otherParamType.equals(this.packageName + "." + paramType)) {
          // The other method already has the FQNs, so do nothing here.
        } else {
          // if there is ever a difference, skip to the next method; there is
          // no need to check for exact equality, because methods is a set
          // so any duplicates won't be added.
          continue methods;
        }
      }
      matchingMethod = otherMethod;
    }

    if (matchingMethod != null) {
      if (preferOther) {
        // Nothing more to do: the correct method is already present,
        // and this one is a duplicate with simple names.
        return;
      } else {
        // We need to replace the current variant of the method with this one.
        // So, remove the current one (the add call below will take care of
        // adding this method, just as if this was a totally new method).
        this.methods.remove(matchingMethod);
      }
    }

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
   * Set the value for preferredTypeVariables.
   *
   * @param preferredTypeVariables desired value for preferredTypeVariables.
   */
  public void setPreferedTypeVariables(Set<String> preferredTypeVariables) {
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
   * Attempts to add an extends clause to this class or (recursively) to one of its inner classes.
   * An extends clause will only be added if the name of this class matches the target type name.
   * The name of the class in the extends clause is extendsName.
   *
   * @param targetTypeName the name of the class to be extended. This may be a fully-qualified name,
   *     a simple name, or a dot-separated identifier.
   * @param extendsName the name of the class to extend. Always fully-qualified.
   * @param visitor the current visitor state
   * @return true if an extends clause was added, false otherwise
   */
  public boolean extend(String targetTypeName, String extendsName, UnsolvedSymbolVisitor visitor) {
    if (targetTypeName.equals(this.getQualifiedClassName())
        || targetTypeName.equals(this.getClassName())) {
      // Special case: if the type to extend is "Annotation", then change the
      // target class to an @interface declaration.
      if ("Annotation".equals(extendsName)
          || "java.lang.annotation.Annotation".equals(extendsName)) {
        setIsAnAnnotationToTrue();
      } else {
        if (!UnsolvedSymbolVisitor.isAClassPath(extendsName)) {
          extendsName = visitor.getPackageFromClassName(extendsName) + "." + extendsName;
        }
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
      result |= unsolvedInnerClass.extend(targetTypeName, extendsName, visitor);
    }
    return result;
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

    classFields.addAll(newFields);
    return successfullyUpdated;
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
    // TODO: this test is very, very bad practice and makes this class
    // not reusable. Find a better way to do this after ISSTA.
    if (this.getClass() != UnsolvedInnerClass.class) {
      sb.append("package ").append(packageName).append(";\n");
    }

    // Synthetic annotations used within generic types cause compile errors,
    // so we need to add this to prevent them
    if (isAnAnnotation) {
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
              + "\tjava.lang.annotation.ElementType.TYPE_USE \n"
              + "})");
    }

    sb.append("public ");
    if (this.getClass() == UnsolvedInnerClass.class) {
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
    for (String variableDeclarations : classFields) {
      sb.append("    " + "public ").append(variableDeclarations).append(";\n");
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
