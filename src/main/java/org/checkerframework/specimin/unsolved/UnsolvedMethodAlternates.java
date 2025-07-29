package org.checkerframework.specimin.unsolved;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.specimin.JavaParserUtil;

/**
 * /** Given a name, return type, a set of parameters and a set of potential encapsulating classes,
 * this class allows for alternates of a same field to be generated in different locations. If a
 * class were:
 *
 * <pre><code>
 * class A extends B implements C {
 *    void x() {
 *      int y = a();
 *    }
 * }
 * </code></pre>
 *
 * where B and C are both unresolvable, method a() could be in either one. Note that {@code
 * getAlternates()} will always return a single alternate for this class, since {@code
 * UnsolvedMethodAlternates} depends on encapsulating classes for alternate definitions.
 */
public class UnsolvedMethodAlternates extends UnsolvedSymbolAlternates<UnsolvedMethod> {
  private UnsolvedMethodAlternates(
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes) {
    super(alternateDeclaringTypes);
  }

  /**
   * Creates a new unsolved method declaration
   *
   * @param name The name of the method
   * @param type The return type of the method
   * @param alternateDeclaringTypes Potential declaring types of the method
   * @param parameters The parameters of the method
   * @return The method definition
   */
  public static UnsolvedMethodAlternates create(
      String name,
      MemberType type,
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes,
      List<MemberType> parameters) {
    // TODO: enable alternate methods in case a method reference is a parameter
    // For example, Foo::bar may refer to a bar(int) -> void or a bar(String) -> boolean
    // If Foo::bar were an argument, we wouldn't know which is which
    return create(name, type, alternateDeclaringTypes, parameters, List.of());
  }

  /**
   * Creates a new unsolved method declaration
   *
   * @param name The name of the method
   * @param type The return type of the method
   * @param alternateDeclaringTypes Potential declaring types of the method
   * @param parameters The parameters of the method
   * @param exceptions Thrown exceptions of this method
   * @return The method definition
   */
  public static UnsolvedMethodAlternates create(
      String name,
      MemberType type,
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes,
      List<MemberType> parameters,
      List<MemberType> exceptions) {
    if (alternateDeclaringTypes.isEmpty()) {
      throw new RuntimeException("Unsolved field must have at least one potential declaring type.");
    }
    UnsolvedMethodAlternates result = new UnsolvedMethodAlternates(alternateDeclaringTypes);

    UnsolvedMethod method = new UnsolvedMethod(name, type, parameters, exceptions);
    result.addAlternate(method);

    return result;
  }

  @Override
  public Set<String> getFullyQualifiedNames() {
    Set<String> fqns = new LinkedHashSet<>();

    for (UnsolvedMethod methodAlternate : getAlternates()) {
      StringBuilder methodSignature = new StringBuilder();

      methodSignature.append(methodAlternate.getName()).append('(');

      List<MemberType> parameterList = methodAlternate.getParameterList();
      for (int i = 0; i < parameterList.size(); i++) {
        MemberType param = parameterList.get(i);

        // This is safe because all simple names are the same for unsolved types
        // and there is only one FQN for solved types
        methodSignature.append(JavaParserUtil.getSimpleNameFromQualifiedName(param.toString()));

        if (i + 1 < parameterList.size()) {
          methodSignature.append(", ");
        }
      }

      methodSignature.append(')');

      for (UnsolvedClassOrInterfaceAlternates alternate : getAlternateDeclaringTypes()) {
        for (String fqn : alternate.getFullyQualifiedNames()) {
          fqns.add(fqn + "#" + methodSignature.toString());
        }
      }
    }

    return fqns;
  }

  /** Makes this method static. */
  public void setIsStaticToTrue() {
    for (UnsolvedMethod method : getAlternates()) {
      method.setStatic();
    }
  }

  /**
   * Sets the number of type variables.
   *
   * @param number The number of type variables
   */
  public void setNumberOfTypeVariables(int number) {
    for (UnsolvedMethod method : getAlternates()) {
      method.setNumberOfTypeVariables(number);
    }
  }

  /**
   * Gets the return type. Note that the return type can also be set via MemberType setter methods.
   *
   * @return The return type
   */
  public MemberType getReturnType() {
    return getAlternates().get(0).getReturnType();
  }
}
