package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
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
 * where B and C are both unresolvable, method a() could be in either one.
 */
public class UnsolvedMethodAlternates extends UnsolvedSymbolAlternates<UnsolvedMethod>
    implements UnsolvedMethodCommon {
  /**
   * Creates a new instance of UnsolvedMethodAlternates. Private constructor; use the create
   * methods.
   *
   * @param alternateDeclaringTypes A list of potential declaring types for this method.
   */
  private UnsolvedMethodAlternates(
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes) {
    super(alternateDeclaringTypes);
  }

  /**
   * Creates a new unsolved method declaration
   *
   * @param name The name of the method
   * @param types The return types of the method
   * @param alternateDeclaringTypes Potential declaring types of the method
   * @param parameters Potential parameters of the method. Each set represents a possibility of
   *     parameter types at that position
   * @return The method definition
   */
  public static UnsolvedMethodAlternates create(
      String name,
      Set<MemberType> types,
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes,
      List<Set<MemberType>> parameters) {
    return create(name, types, alternateDeclaringTypes, parameters, List.of());
  }

  /**
   * Creates a new unsolved method declaration
   *
   * @param name The name of the method
   * @param types The return types of the method
   * @param alternateDeclaringTypes Potential declaring types of the method
   * @param parameters Potential parameters of the method. Each set represents a possibility of
   *     parameter types at that position
   * @param exceptions Thrown exceptions of this method
   * @return The method definition
   */
  public static UnsolvedMethodAlternates create(
      String name,
      Set<MemberType> types,
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes,
      List<Set<MemberType>> parameters,
      List<MemberType> exceptions) {
    return create(name, types, alternateDeclaringTypes, parameters, exceptions, "public");
  }

  /**
   * Creates a new unsolved method declaration
   *
   * @param name The name of the method
   * @param types The return types of the method
   * @param alternateDeclaringTypes Potential declaring types of the method
   * @param parameters Potential parameters of the method. Each set represents a possibility of
   *     parameter types at that position
   * @param exceptions Thrown exceptions of this method
   * @param accessModifier The access modifier of this method
   * @return The method definition
   */
  public static UnsolvedMethodAlternates create(
      String name,
      Set<MemberType> types,
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes,
      List<Set<MemberType>> parameters,
      List<MemberType> exceptions,
      String accessModifier) {
    if (alternateDeclaringTypes.isEmpty()) {
      throw new RuntimeException(
          "Unsolved method must have at least one potential declaring type.");
    }

    if (types.isEmpty()) {
      throw new RuntimeException("Unsolved method must have at least one potential return type.");
    }

    UnsolvedMethodAlternates result = new UnsolvedMethodAlternates(alternateDeclaringTypes);

    for (List<MemberType> parameterList : JavaParserUtil.generateAllCombinations(parameters)) {
      for (MemberType type : types) {
        UnsolvedMethod method =
            new UnsolvedMethod(name, type, parameterList, exceptions, Set.of(), accessModifier);
        result.addAlternate(method);
      }
    }

    return result;
  }

  /**
   * Creates a new unsolved method declaration
   *
   * @param name The name of the method
   * @param types The return types of the method
   * @param alternateDeclaringTypes Potential declaring types of the method
   * @param parameters Potential parameters of the method. Each map represents a possibility of
   *     parameter types at that position, along with nodes that must be preserved if that type is
   *     chosen
   * @param exceptions Thrown exceptions of this method
   * @return The method definition
   */
  public static UnsolvedMethodAlternates createWithPreservation(
      String name,
      Set<MemberType> types,
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes,
      List<Map<MemberType, @Nullable Node>> parameters,
      List<MemberType> exceptions) {
    if (alternateDeclaringTypes.isEmpty()) {
      throw new RuntimeException(
          "Unsolved method must have at least one potential declaring type.");
    }

    if (types.isEmpty()) {
      throw new RuntimeException("Unsolved method must have at least one potential return type.");
    }

    UnsolvedMethodAlternates result = new UnsolvedMethodAlternates(alternateDeclaringTypes);

    for (List<Map.Entry<MemberType, @Nullable Node>> parameterList :
        JavaParserUtil.generateAllCombinationsForListOfMaps(parameters)) {
      for (MemberType type : types) {
        List<MemberType> params = parameterList.stream().map(Map.Entry::getKey).toList();
        Set<Node> toPreserve = new HashSet<>();

        for (Map.Entry<MemberType, @Nullable Node> entry : parameterList) {
          Node node = entry.getValue();
          if (node != null) {
            toPreserve.add(node);
          }
        }

        UnsolvedMethod method = new UnsolvedMethod(name, type, params, exceptions, toPreserve);
        result.addAlternate(method);
      }
    }

    return result;
  }

  /**
   * Creates a new unsolved method declaration
   *
   * @param name The name of the method
   * @param returnTypesToMustPreserveNodes A map of return types to must-preserve nodes. Different
   *     return types may lead to different sets of nodes that need to be conditionally preserved.
   * @param alternateDeclaringTypes Potential declaring types of the method
   * @param parameters Potential parameters of the method. Each map represents a possibility of
   *     parameter types at that position, along with nodes that must be preserved if that type is
   *     chosen
   * @param exceptions Thrown exceptions of this method
   * @return The method definition
   */
  public static UnsolvedMethodAlternates createWithPreservation(
      String name,
      Map<MemberType, CallableDeclaration<?>> returnTypesToMustPreserveNodes,
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes,
      List<Map<MemberType, @Nullable Node>> parameters,
      List<MemberType> exceptions) {
    if (alternateDeclaringTypes.isEmpty()) {
      throw new RuntimeException(
          "Unsolved method must have at least one potential declaring type.");
    }
    UnsolvedMethodAlternates result = new UnsolvedMethodAlternates(alternateDeclaringTypes);

    for (List<Map.Entry<MemberType, @Nullable Node>> parameterList :
        JavaParserUtil.generateAllCombinationsForListOfMaps(parameters)) {
      for (Map.Entry<MemberType, CallableDeclaration<?>> returnType :
          returnTypesToMustPreserveNodes.entrySet()) {
        List<MemberType> params = parameterList.stream().map(Map.Entry::getKey).toList();
        Set<Node> toPreserve = new HashSet<>();
        for (Map.Entry<MemberType, @Nullable Node> entry : parameterList) {
          Node node = entry.getValue();
          if (node != null) {
            toPreserve.add(node);
          }
        }

        Node returnTypePreserve = returnType.getValue();

        if (returnTypePreserve != null) {
          toPreserve.add(returnTypePreserve);
        }

        UnsolvedMethod method =
            new UnsolvedMethod(name, returnType.getKey(), params, exceptions, toPreserve);
        result.addAlternate(method);
      }
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
    Set<MemberType> oldReturnTypes = getReturnTypes();
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
  public Set<MemberType> getReturnTypes() {
    return getAlternates().stream()
        .map(alternate -> alternate.getReturnType())
        .collect(Collectors.toCollection(LinkedHashSet::new));
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

  @Override
  public boolean isStatic() {
    return getAlternates().get(0).isStatic();
  }
}
