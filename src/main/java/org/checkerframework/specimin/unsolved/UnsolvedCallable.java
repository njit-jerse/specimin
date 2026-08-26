package org.checkerframework.specimin.unsolved;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;
import org.checkerframework.specimin.JavaParserUtil;

/**
 * A synthetic method or constructor declaration: everything a declaration has whether or not it is
 * a constructor. {@link UnsolvedMethod} adds a name and a return type; {@link UnsolvedConstructor}
 * adds neither, because JLS 8.8.1 gives a constructor no return type and fixes its name to the
 * simple name of the type that declares it.
 *
 * <p>The pair matches JavaParser's own split, where {@code MethodDeclaration} and {@code
 * ConstructorDeclaration} are both {@code CallableDeclaration}s.
 */
public abstract class UnsolvedCallable extends UnsolvedSymbolAlternate
    implements UnsolvedCallableCommon {
  /** The list of the types of the parameters of this callable. */
  private final List<MemberType> parameterList;

  /** This field is set to true if this is a static method. */
  private boolean isStatic;

  /** The list of the types of the exceptions thrown by this callable. */
  private final List<MemberType> throwsList;

  /**
   * The names of this callable's type variables, in declaration order.
   *
   * <p>These names are the state. Most of a callable's type variables are invented by Specimin and
   * named by {@link JavaParserUtil#getGeneratedTypeParameterName}, but one that {@link
   * #declareTypeVariables} binds carries a name that is already written into the signature and
   * cannot be renamed.
   */
  private final List<String> typeVariableNames;

  /** The access modifier of this callable. */
  private String accessModifier;

  /** The body of this callable. */
  private String content = "throw new java.lang.Error();";

  /**
   * Base constructor for a synthetic callable.
   *
   * @param parameterList the list of parameters
   * @param throwsList the list of exceptions thrown
   * @param mustPreserve the set of nodes that must be preserved with this alternate
   * @param accessModifier the access modifier
   * @param isStatic whether this is static
   * @param typeVariableNames the names of the type variables, in declaration order
   */
  protected UnsolvedCallable(
      List<MemberType> parameterList,
      List<MemberType> throwsList,
      Set<Node> mustPreserve,
      String accessModifier,
      boolean isStatic,
      List<String> typeVariableNames) {
    super(mustPreserve);

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
  protected static List<String> generatedTypeVariableNames(int count) {
    List<String> names = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      names.add(JavaParserUtil.getGeneratedTypeParameterName(i));
    }
    return names;
  }

  /**
   * Returns the name under which this callable is declared. A method is declared under its own
   * name; a constructor is declared under the simple name of the type that declares it (JLS 8.8.1),
   * which is why the declaring type's name has to be supplied here rather than stored.
   *
   * @param declaringTypeName the simple name of the type that declares this callable
   * @return the name to declare this callable under
   */
  protected abstract String declaredName(@ClassGetSimpleName String declaringTypeName);

  /**
   * Returns the text that precedes the declared name in this callable's signature: the return type
   * followed by a space for a method, and nothing at all for a constructor, which declares no
   * return type (JLS 8.8.1).
   *
   * @return the return type to print, followed by a space, or the empty string
   */
  protected abstract String returnTypePrefix();

  /**
   * Returns the types written into this callable's signature, which are the types that can mention
   * its type variables: the parameter types, plus a method's return type.
   *
   * @return the types in this callable's signature
   */
  protected List<MemberType> typesInSignature() {
    return getParameterList();
  }

  /**
   * Get the return type of this callable.
   *
   * @return the return type
   * @throws UnsupportedOperationException if this is a constructor, which declares no return type
   */
  public abstract MemberType getReturnType();

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

  /**
   * Return the content of this callable. Note that the body is stubbed out.
   *
   * @param type The type of the declaring type
   * @param declaringTypeName The simple name of the declaring type, which is the name a constructor
   *     is declared under (JLS 8.8.1)
   * @return the declaration with the body stubbed out
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

    signature.append(returnTypePrefix());
    signature.append(declaredName(declaringTypeName)).append("(");
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
   * Returns the names of this callable's type variables, in declaration order. Pass this to a
   * subclass constructor when copying: a name that {@link #declareTypeVariables} bound cannot be
   * reconstructed from a count.
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

    // A type variable may be allocated for this callable but end up used by no alternate's
    // signature, in which case there is no type parameter section to print at all.
    if (used.isEmpty()) {
      return "";
    }

    return "<" + String.join(", ", used) + ">";
  }

  /** Gets a list of the type variable names that are used in this callable. */
  private List<String> getTypeVariablesImpl() {
    // While it is better to have the exact type number of type variables
    // on a per-alternate basis, it's easier for other parts of this codebase
    // to deal with the same number of type variables for all alternates of a given method.
    // So, we'll deal with that limitation here and not include any type variables
    // that are not used in this specific alternate.

    // Parse to properly handle type arguments (i.e., a class named TheUnsolved should not be
    // considered to use T)
    List<ClassOrInterfaceType> usedTypes = new ArrayList<>();
    for (MemberType signatureType : typesInSignature()) {
      Type parsedType = StaticJavaParser.parseType(signatureType.toString());

      if (!parsedType.isClassOrInterfaceType()) {
        continue;
      }

      usedTypes.add(parsedType.asClassOrInterfaceType());
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
