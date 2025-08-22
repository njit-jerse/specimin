package org.checkerframework.specimin.unsolved;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;

/** Represents a single unsolved class or interface alternate. */
public class UnsolvedClassOrInterface extends UnsolvedSymbolAlternate
    implements UnsolvedClassOrInterfaceCommon {

  /** The name of the class */
  private final @ClassGetSimpleName String className;

  /**
   * The name of the package of the class. We rely on the import statements from the source codes to
   * guess the package name.
   */
  private final String packageName;

  /** The type variables, if any exist. */
  private List<String> typeVariables = Collections.emptyList();

  /** The extends clause, if one exists. */
  private @Nullable MemberType extendsClause;

  /** The implements clauses, if they exist. */
  private Set<MemberType> implementsClauses = new LinkedHashSet<>(0);

  private Set<String> annotations = new HashSet<>();

  /** The type of this type; i.e., is it a class, interface, annotation, enum? */
  private UnsolvedClassOrInterfaceType typeOfType = UnsolvedClassOrInterfaceType.UNKNOWN;

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
   * Adds new interfaces to the set of implemented interfaces.
   *
   * @param interfaceTypes The interface types
   */
  public void implement(Collection<MemberType> interfaceTypes) {
    implementsClauses.addAll(interfaceTypes);
  }

  /**
   * Adds a new interface to the set of implemented interfaces.
   *
   * @param interfaceType The interface type
   */
  public void implement(MemberType interfaceType) {
    implementsClauses.add(interfaceType);
  }

  /**
   * Checks if an interface is implemented or not.
   *
   * @param interfaceType the fqn of the interface
   */
  @Override
  public boolean doesImplement(MemberType interfaceType) {
    return implementsClauses.contains(interfaceType);
  }

  /**
   * Adds an extends clause to this class.
   *
   * @param extendsType a {@link MemberType} of the extended type, represented with fully qualified
   *     names.
   */
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
    copy.typeVariables = new ArrayList<>(this.typeVariables);
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
   * @param methods the methods of the class
   * @param fields the fields of the class
   * @param innerClassDefinitions the inner classes of the class
   * @param isInnerClass whether this class is an inner class
   * @return the content of the class
   */
  public String toString(
      Collection<UnsolvedMethod> methods,
      Collection<UnsolvedField> fields,
      Collection<String> innerClassDefinitions,
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
    sb.append(className);

    if (!getTypeVariables().isEmpty()) {
      sb.append("<");
      sb.append(String.join(", ", getTypeVariables()));
      sb.append(">");
    }

    if (extendsClause != null) {
      @NonNull MemberType nonNullExtends = extendsClause;
      sb.append(" extends ").append(nonNullExtends).append(" ");
    }
    if (implementsClauses.size() > 0) {
      if (typeOfType == UnsolvedClassOrInterfaceType.INTERFACE) {
        if (extendsClause != null) {
          sb.append(", ");
        } else {
          sb.append(" extends ");
        }
      } else {
        sb.append(" implements ");
      }
      Iterator<MemberType> interfaces = implementsClauses.iterator();
      while (interfaces.hasNext()) {
        sb.append(interfaces.next());
        if (interfaces.hasNext()) {
          sb.append(", ");
        }
      }
    }
    sb.append(" {\n");
    if (innerClassDefinitions != null) {
      for (String innerClass : innerClassDefinitions) {
        sb.append(innerClass);
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
   * Return a synthetic representation for type variables of the current class, without surrounding
   * angle brackets.
   *
   * @return the synthetic representation for type variables
   */
  @Override
  public List<String> getTypeVariables() {
    return typeVariables;
  }

  @Override
  public UnsolvedClassOrInterfaceType getType() {
    return typeOfType;
  }

  /**
   * Gets the set of types that are implemented by this type.
   *
   * @return The implemented types
   */
  public Set<MemberType> getImplementedTypes() {
    return implementsClauses;
  }

  /**
   * Gets the extended type of this type.
   *
   * @return The extended type, or null if none
   */
  public @Nullable MemberType getExtendedType() {
    return extendsClause;
  }

  @Override
  public void setType(UnsolvedClassOrInterfaceType type) {
    typeOfType = type;
  }

  @Override
  public void setTypeVariables(List<String> typeVariables) {
    this.typeVariables = typeVariables;
  }

  @Override
  public void setTypeVariables(int numberOfTypeVariables) {
    List<String> result = new ArrayList<>();

    for (int i = 0; i < numberOfTypeVariables; i++) {
      String typeExpression = "T" + ((i > 0) ? i : "");
      result.add(typeExpression);
    }

    setTypeVariables(result);
  }
}
