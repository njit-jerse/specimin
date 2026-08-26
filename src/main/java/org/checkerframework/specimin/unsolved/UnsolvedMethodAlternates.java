package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithParameters;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.specimin.JavaParserUtil;

/**
 * Alternates of one synthetic method: everything {@link UnsolvedCallableAlternates} has, plus the
 * name and return type that only a method has. See {@link UnsolvedConstructorAlternates} for the
 * constructor case.
 */
public class UnsolvedMethodAlternates extends UnsolvedCallableAlternates<UnsolvedMethod> {
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
    UnsolvedMethodAlternates result = checkAndCreate(alternateDeclaringTypes, types);

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
    UnsolvedMethodAlternates result = checkAndCreate(alternateDeclaringTypes, types);

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
      Map<MemberType, NodeWithParameters<?>> returnTypesToMustPreserveNodes,
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
      for (Map.Entry<MemberType, NodeWithParameters<?>> returnType :
          returnTypesToMustPreserveNodes.entrySet()) {
        List<MemberType> params = parameterList.stream().map(Map.Entry::getKey).toList();
        Set<Node> toPreserve = new HashSet<>();
        for (Map.Entry<MemberType, @Nullable Node> entry : parameterList) {
          Node node = entry.getValue();
          if (node != null) {
            toPreserve.add(node);
          }
        }

        Node returnTypePreserve = (Node) returnType.getValue();

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
   * Checks that there is at least one declaring type and one return type, and creates an empty
   * instance.
   *
   * @param alternateDeclaringTypes Potential declaring types of the method
   * @param types The return types of the method
   * @return an instance with no alternates yet
   */
  private static UnsolvedMethodAlternates checkAndCreate(
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes, Set<MemberType> types) {
    if (alternateDeclaringTypes.isEmpty()) {
      throw new RuntimeException(
          "Unsolved method must have at least one potential declaring type.");
    }

    if (types.isEmpty()) {
      throw new RuntimeException("Unsolved method must have at least one potential return type.");
    }

    return new UnsolvedMethodAlternates(alternateDeclaringTypes);
  }

  /**
   * Updates return types and must preserve nodes. Saves the intersection of the previous and the
   * input, since we know more information to narrow potential return types down.
   *
   * @param returnsToPreserveNodes A map of return types to nodes that must be preserved
   */
  public void updateReturnTypesAndMustPreserveNodes(
      Map<MemberType, NodeWithParameters<?>> returnsToPreserveNodes) {
    // Update in-place; intersection = removing all elements in the original set
    // that isn't found in the updated set
    UnsolvedMethod old = getAlternates().get(0);

    Set<List<MemberType>> parameterLists =
        getAlternates().stream()
            .map(UnsolvedMethod::getParameterList)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    getAlternates()
        .removeIf(alternate -> !returnsToPreserveNodes.containsKey(alternate.getReturnType()));

    if (getAlternates().isEmpty()) {
      for (Map.Entry<MemberType, NodeWithParameters<?>> entry : returnsToPreserveNodes.entrySet()) {
        for (List<MemberType> parameterList : parameterLists) {
          UnsolvedMethod method =
              new UnsolvedMethod(
                  old.getName(),
                  entry.getKey(),
                  parameterList,
                  old.getThrownExceptions(),
                  Set.of((Node) entry.getValue()),
                  old.getAccessModifier(),
                  old.isStatic(),
                  old.getTypeVariableNames());
          addAlternate(method);
        }
      }
    }
  }

  /** Sets the return type of this method to be an unconstrained type variable. */
  public void setUnconstrainedReturnType() {
    // Remove all alternates based on return type
    Set<List<MemberType>> parameterLists = new HashSet<>();

    for (int i = 0; i < getAlternates().size(); i++) {
      UnsolvedMethod alternate = getAlternates().get(i);
      if (parameterLists.contains(alternate.getParameterList())) {
        getAlternates().remove(i);
        i--;
      }

      parameterLists.add(alternate.getParameterList());
    }

    for (UnsolvedMethod alternate : getAlternates()) {
      alternate.setNumberOfTypeVariables(alternate.getNumberOfTypeVariables() + 1);
      MemberType returnType =
          new SolvedMemberType(
              alternate.getTypeVariableName(alternate.getNumberOfTypeVariables() - 1));
      alternate.setReturnType(returnType);
    }
  }

  /**
   * Gets the return types
   *
   * @return The return types
   */
  public Set<MemberType> getReturnTypes() {
    return getAlternates().stream()
        .map(UnsolvedMethod::getReturnType)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  /**
   * Gets the name of this method.
   *
   * @return the name
   */
  public String getName() {
    return getAlternates().get(0).getName();
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

  /**
   * For all the alternates that have oldType as return type, replace it with newType.
   *
   * @param oldType The old return type
   * @param newType The new return type
   */
  public void replaceReturnType(MemberType oldType, Set<MemberType> newType) {
    if (newType.isEmpty()) {
      throw new RuntimeException("New return type set cannot be empty.");
    }

    // Replacing the current old type with the first new type and then adding the rest
    // of the return types effectively removes the original old type.
    replaceReturnType(oldType, newType.iterator().next());
    addReturnTypes(newType);
  }

  /**
   * Adds a new alternate with the same name, parameters and thrown exceptions, but with a different
   * return type.
   *
   * @param returnTypes The new return types to add.
   */
  public void addReturnTypes(Set<MemberType> returnTypes) {
    Set<List<MemberType>> seenParameterLists = new HashSet<>();
    int originalSize = getAlternates().size();
    for (int i = 0; i < originalSize; i++) {
      UnsolvedMethod alternate = getAlternates().get(i);
      if (seenParameterLists.contains(alternate.getParameterList())) {
        return;
      }

      for (MemberType returnType : returnTypes) {
        if (alternate.getReturnType().equals(returnType)) {
          continue;
        }

        UnsolvedMethod newAlternate =
            new UnsolvedMethod(
                alternate.getName(),
                returnType,
                alternate.getParameterList(),
                alternate.getThrownExceptions(),
                alternate.getMustPreserveNodes(),
                alternate.getAccessModifier(),
                alternate.isStatic(),
                alternate.getTypeVariableNames());
        addAlternate(newAlternate);
      }

      seenParameterLists.add(alternate.getParameterList());
    }
  }

  @Override
  protected UnsolvedMethod copyWithParameters(
      UnsolvedMethod alternate, List<MemberType> parameterList) {
    return new UnsolvedMethod(
        alternate.getName(),
        alternate.getReturnType(),
        parameterList,
        alternate.getThrownExceptions(),
        alternate.getMustPreserveNodes(),
        alternate.getAccessModifier(),
        alternate.isStatic(),
        alternate.getTypeVariableNames());
  }
}
