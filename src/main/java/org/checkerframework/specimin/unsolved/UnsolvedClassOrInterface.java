package org.checkerframework.specimin.unsolved;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;

public class UnsolvedClassOrInterface extends UnsolvedSymbolAlternate
    implements UnsolvedClassOrInterfaceCommon {

  /** The name of the class */
  private final @ClassGetSimpleName String className;

  /**
   * The name of the package of the class. We rely on the import statements from the source codes to
   * guess the package name.
   */
  private final String packageName;

  /** The number of type variables for this class */
  private int numberOfTypeVariables = 0;

  /** The extends clause, if one exists. */
  private @Nullable MemberType extendsClause;

  /** The implements clauses, if they exist. */
  private Set<String> implementsClauses = new LinkedHashSet<>(0);

  private Set<String> annotations = new HashSet<>();

  /** The type of this type; i.e., is it a class, interface, annotation, enum? */
  private UnsolvedClassOrInterfaceType typeOfType = UnsolvedClassOrInterfaceType.CLASS;

  /**
   * This constructor correctly splits apart the class name and any generics attached to it.
   *
   * @param className the name of the class, possibly followed by a set of type arguments
   * @param packageName the name of the package
   */
  public UnsolvedClassOrInterface(String className, String packageName) {
    // Types do not have mustPreserve nodes
    super(Set.of());
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
  }

  /**
   * Get the name of this class (note: without any generic type variables).
   *
   * @return the name of the class
   */
  @Override
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
  @Override
  public void setNumberOfTypeVariables(int numberOfTypeVariables) {
    this.numberOfTypeVariables = numberOfTypeVariables;
  }

  /**
   * This method tells the number of type variables for this class
   *
   * @return the number of type variables
   */
  @Override
  public int getNumberOfTypeVariables() {
    return this.numberOfTypeVariables;
  }

  /**
   * Adds a new interface to the list of implemented interfaces.
   *
   * @param interfaceName the fqn of the interface
   */
  @Override
  public void implement(String interfaceName) {
    implementsClauses.add(interfaceName);
  }

  /**
   * Checks if an interface is implemented or not.
   *
   * @param interfaceName the fqn of the interface
   */
  @Override
  public boolean doesImplement(String interfaceName) {
    return implementsClauses.contains(interfaceName);
  }

  /**
   * Adds an extends clause to this class.
   *
   * @param extendsType a {@link MemberType} of the extended type, represented with fully qualified
   *     names.
   */
  @Override
  public void extend(MemberType extendsType) {
    this.extendsClause = extendsType;
  }

  /**
   * Returns true if this class extends another class.
   *
   * @return whether {@code this.extendsClause} is non-null
   */
  @Override
  public boolean hasExtends() {
    return this.extendsClause != null;
  }

  /**
   * Checks if the extended class is equal to the input.
   *
   * @param extendsType a fully-qualified class name for the extended class
   * @return whether {@code className} is the extended class of this
   */
  @Override
  public boolean doesExtend(MemberType extendsType) {
    return this.extendsClause != null && this.extendsClause.equals(extendsType);
  }

  /**
   * Adds an annotation to this class.
   *
   * @param annotation a fully-qualified annotation to apply
   */
  @Override
  public void addAnnotation(String annotation) {
    this.annotations.add(annotation);
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

  /**
   * Returns a copy of this class.
   *
   * @return A copy of the current instance
   */
  public UnsolvedClassOrInterface copy() {
    UnsolvedClassOrInterface copy = new UnsolvedClassOrInterface(className, packageName);

    copy.extendsClause = this.extendsClause;
    copy.implementsClauses = new LinkedHashSet<>(this.implementsClauses);
    copy.typeOfType = this.typeOfType;
    copy.numberOfTypeVariables = this.numberOfTypeVariables;
    copy.annotations = new HashSet<>(this.annotations);

    return copy;
  }

  @Override
  public String toString() {
    return toString(
        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false);
  }

  /**
   * Return the content of the class as a compilable Java file.
   *
   * @return the content of the class
   */
  public String toString(
      Collection<UnsolvedMethod> methods,
      Collection<UnsolvedField> fields,
      Collection<UnsolvedClassOrInterface> innerClasses,
      boolean isInnerClass) {
    StringBuilder sb = new StringBuilder();
    if (!isInnerClass) {
      sb.append("package ").append(packageName).append(";\n");
    }

    for (String annotation : annotations) {
      sb.append(annotation).append("\n");
    }

    sb.append("public ");
    if (isInnerClass) {
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
    if (typeOfType == UnsolvedClassOrInterfaceType.INTERFACE) {
      sb.append("interface ");
    } else if (typeOfType == UnsolvedClassOrInterfaceType.ANNOTATION) {
      sb.append("@interface ");
    } else if (typeOfType == UnsolvedClassOrInterfaceType.ENUM) {
      sb.append("enum ");
    } else {
      sb.append("class ");
    }
    sb.append(className).append(getTypeVariablesAsString());
    if (extendsClause != null) {
      @NonNull MemberType nonNullExtends = extendsClause;
      sb.append(" extends ").append(nonNullExtends).append(" ");
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
      sb.append("    ").append(variableDeclarations.toString(typeOfType)).append("\n");
    }
    for (UnsolvedMethod method : methods) {
      sb.append(method.toString(typeOfType));
    }
    sb.append("}\n");
    return sb.toString();
  }

  /**
   * Return a synthetic representation for type variables of the current class.
   *
   * @return the synthetic representation for type variables
   */
  @Override
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
  @Override
  public String getTypeVariablesAsStringWithoutBrackets() {
    if (numberOfTypeVariables == 0) {
      return "";
    }
    StringBuilder result = new StringBuilder();
    getTypeVariablesImpl(result);
    return result.toString();
  }

  /**
   * Helper method for {@link #getTypeVariablesAsStringWithoutBrackets()} and {@link
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

  @Override
  public UnsolvedClassOrInterfaceType getType() {
    return typeOfType;
  }

  @Override
  public void setType(UnsolvedClassOrInterfaceType type) {
    typeOfType = type;
  }
}
