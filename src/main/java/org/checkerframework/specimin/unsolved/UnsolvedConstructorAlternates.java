package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.Node;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.specimin.JavaParserUtil;

/**
 * Alternates of one synthetic constructor. There is no name or return type here, nor a parameter
 * for either in the factories: JLS 8.8.1 gives a constructor no return type and requires it to be
 * declared under the simple name of its declaring type, so both are read from that type rather than
 * supplied by a caller.
 */
public class UnsolvedConstructorAlternates extends UnsolvedCallableAlternates<UnsolvedConstructor> {
  /**
   * Creates a new instance of UnsolvedConstructorAlternates. Private constructor; use the create
   * methods.
   *
   * @param declaringTypes A list of potential declaring types for this constructor.
   */
  private UnsolvedConstructorAlternates(List<UnsolvedClassOrInterfaceAlternates> declaringTypes) {
    super(declaringTypes);
  }

  /**
   * Creates a new unsolved constructor declaration.
   *
   * @param declaringTypes The types whose constructor this could be
   * @param parameters Potential parameters of the constructor. Each set represents a possibility of
   *     parameter types at that position
   * @return The constructor definition
   */
  public static UnsolvedConstructorAlternates create(
      List<UnsolvedClassOrInterfaceAlternates> declaringTypes, List<Set<MemberType>> parameters) {
    UnsolvedConstructorAlternates result = checkAndCreate(declaringTypes);

    for (List<MemberType> parameterList : JavaParserUtil.generateAllCombinations(parameters)) {
      result.addAlternate(
          new UnsolvedConstructor(parameterList, List.of(), Set.of(), "public", List.of()));
    }

    return result;
  }

  /**
   * Creates a new unsolved constructor declaration, recording nodes that must be preserved if a
   * given parameter type is chosen.
   *
   * @param declaringTypes The types whose constructor this could be
   * @param parameters Potential parameters of the constructor. Each map represents a possibility of
   *     parameter types at that position, along with nodes that must be preserved if that type is
   *     chosen
   * @return The constructor definition
   */
  public static UnsolvedConstructorAlternates createWithPreservation(
      List<UnsolvedClassOrInterfaceAlternates> declaringTypes,
      List<Map<MemberType, @Nullable Node>> parameters) {
    UnsolvedConstructorAlternates result = checkAndCreate(declaringTypes);

    for (List<Map.Entry<MemberType, @Nullable Node>> parameterList :
        JavaParserUtil.generateAllCombinationsForListOfMaps(parameters)) {
      List<MemberType> params = parameterList.stream().map(Map.Entry::getKey).toList();
      Set<Node> toPreserve = new HashSet<>();

      for (Map.Entry<MemberType, @Nullable Node> entry : parameterList) {
        Node node = entry.getValue();
        if (node != null) {
          toPreserve.add(node);
        }
      }

      result.addAlternate(
          new UnsolvedConstructor(params, List.of(), toPreserve, "public", List.of()));
    }

    return result;
  }

  /**
   * Checks that there is at least one declaring type and creates an empty instance.
   *
   * @param declaringTypes The types whose constructor this could be
   * @return an instance with no alternates yet
   */
  private static UnsolvedConstructorAlternates checkAndCreate(
      List<UnsolvedClassOrInterfaceAlternates> declaringTypes) {
    if (declaringTypes.isEmpty()) {
      throw new RuntimeException(
          "Unsolved constructor must have at least one potential declaring type.");
    }

    return new UnsolvedConstructorAlternates(declaringTypes);
  }

  @Override
  protected UnsolvedConstructor copyWithParameters(
      UnsolvedConstructor alternate, List<MemberType> parameterList) {
    return new UnsolvedConstructor(
        parameterList,
        alternate.getThrownExceptions(),
        alternate.getMustPreserveNodes(),
        alternate.getAccessModifier(),
        alternate.getTypeVariableNames());
  }
}
