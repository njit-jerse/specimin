package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.body.CallableDeclaration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
public class UnsolvedMethodAlternates extends UnsolvedSymbolAlternates<UnsolvedMethod>
    implements UnsolvedMethodCommon {
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
    // TODO: enable alternate methods in case a method reference is an argument
    // For example, Foo::bar may refer to a bar(int) -> void or a bar(String) -> boolean
    // If Foo::bar were an argument, we wouldn't know which is which

    // TODO: alternates for default/abstract methods in interfaces
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
    return create(name, type, alternateDeclaringTypes, parameters, exceptions, "public");
  }

  /**
   * Creates a new unsolved method declaration
   *
   * @param name The name of the method
   * @param type The return type of the method
   * @param alternateDeclaringTypes Potential declaring types of the method
   * @param parameters The parameters of the method
   * @param exceptions Thrown exceptions of this method
   * @param accessModifier The access modifier of this method
   * @return The method definition
   */
  public static UnsolvedMethodAlternates create(
      String name,
      MemberType type,
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes,
      List<MemberType> parameters,
      List<MemberType> exceptions,
      String accessModifier) {
    if (alternateDeclaringTypes.isEmpty()) {
      throw new RuntimeException(
          "Unsolved method must have at least one potential declaring type.");
    }
    UnsolvedMethodAlternates result = new UnsolvedMethodAlternates(alternateDeclaringTypes);

    UnsolvedMethod method =
        new UnsolvedMethod(name, type, parameters, exceptions, Set.of(), accessModifier);
    result.addAlternate(method);

    return result;
  }

  /**
   * Creates a new unsolved method declaration
   *
   * @param name The name of the method
   * @param returnTypesToMustPreserveNodes A map of return types to must-preserve nodes. Different
   *     return types may lead to different sets of nodes that need to be conditionally preserved.
   * @param alternateDeclaringTypes Potential declaring types of the method
   * @param parameters The parameters of the method
   * @param exceptions Thrown exceptions of this method
   * @return The method definition
   */
  public static UnsolvedMethodAlternates create(
      String name,
      Map<MemberType, CallableDeclaration<?>> returnTypesToMustPreserveNodes,
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes,
      List<MemberType> parameters,
      List<MemberType> exceptions) {
    if (alternateDeclaringTypes.isEmpty()) {
      throw new RuntimeException(
          "Unsolved method must have at least one potential declaring type.");
    }
    UnsolvedMethodAlternates result = new UnsolvedMethodAlternates(alternateDeclaringTypes);

    for (Map.Entry<MemberType, CallableDeclaration<?>> entry :
        returnTypesToMustPreserveNodes.entrySet()) {
      UnsolvedMethod method =
          new UnsolvedMethod(
              name, entry.getKey(), parameters, exceptions, Set.of(entry.getValue()));
      result.addAlternate(method);
    }

    return result;
  }

  /**
   * Updates return types and must preserve nodes. Saves the intersection of the previous and the
   * input, since we know more information to narrow potential return types down.
   *
   * @param returnsToPreserveNodes A map of return types to nodes that must be preserved
   */
  public void updateReturnTypesAndMustPreserveNodes(
      Map<MemberType, CallableDeclaration<?>> returnsToPreserveNodes) {
    // Update in-place; intersection = removing all elements in the original set
    // that isn't found in the updated set
    UnsolvedMethod old = getAlternates().get(0);
    List<MemberType> oldReturnTypes = getReturnTypes();
    getAlternates()
        .removeIf(alternate -> !returnsToPreserveNodes.containsKey(alternate.getReturnType()));

    if (getAlternates().isEmpty() && oldReturnTypes.size() == 1) {
      // If it's now empty and old return types was of size 1, it was probably a synthetic return
      // type
      for (Map.Entry<MemberType, CallableDeclaration<?>> entry :
          returnsToPreserveNodes.entrySet()) {
        UnsolvedMethod method =
            new UnsolvedMethod(
                old.getName(),
                entry.getKey(),
                old.getParameterList(),
                old.getThrownExceptions(),
                Set.of(entry.getValue()));
        addAlternate(method);
      }
    }
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
        methodSignature.append(
            JavaParserUtil.getSimpleNameFromQualifiedName(JavaParserUtil.erase(param.toString())));

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
  @Override
  public void setStatic() {
    applyToAllAlternates(UnsolvedMethod::setStatic);
  }

  /**
   * Gets the number of type variables.
   *
   * @return The number of type variables
   */
  @Override
  public int getNumberOfTypeVariables() {
    return getAlternates().get(0).getNumberOfTypeVariables();
  }

  /**
   * Sets the number of type variables.
   *
   * @param number The number of type variables
   */
  @Override
  public void setNumberOfTypeVariables(int number) {
    applyToAllAlternates(UnsolvedMethod::setNumberOfTypeVariables, number);
  }

  /**
   * Gets the return types
   *
   * @return The return types
   */
  public List<MemberType> getReturnTypes() {
    return getAlternates().stream().map(alternate -> alternate.getReturnType()).toList();
  }

  @Override
  public String getName() {
    return getAlternates().get(0).getName();
  }

  @Override
  public List<MemberType> getThrownExceptions() {
    return getAlternates().get(0).getThrownExceptions();
  }

  /**
   * Use with caution: this method sets all alternates' return types to the same type.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public void setReturnType(MemberType memberType) {
    applyToAllAlternates(UnsolvedMethod::setReturnType, memberType);
  }

  /**
   * For all the alternates that have oldType as return type, replace it with newType.
   *
   * @param oldType The old return type
   * @param newType The new return type
   */
  public void replaceReturnType(MemberType oldType, MemberType newType) {
    for (UnsolvedMethod alternate : getAlternates()) {
      if (alternate.getReturnType().equals(oldType)) {
        alternate.setReturnType(newType);
      }
    }
  }

  @Override
  public String getAccessModifier() {
    return getAlternates().get(0).getAccessModifier();
  }

  @Override
  public void setAccessModifier(String accessModifier) {
    applyToAllAlternates(UnsolvedMethod::setAccessModifier, accessModifier);
  }
}
