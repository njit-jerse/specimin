package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.nodeTypes.NodeWithParameters;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.specimin.JavaParserUtil;
import org.checkerframework.specimin.QualifiedTypeName;

/**
 * /** Given a name, return type, a set of parameters and a set of potential encapsulating classes,
 * this class allows for alternates of the same method to be generated in different locations. If a
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
 *
 * <p>For type parameters, you may always assume the following convention: T, T1, T2, ...
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
   * Creates a new unsolved constructor declaration. There is no name parameter: JLS 8.8.1 requires
   * a constructor to be declared under the simple name of its declaring type, so the name is read
   * from the declaring type rather than supplied by the caller. Neither is there a return type
   * parameter: a constructor declares none, which {@link UnsolvedMethod#toString} represents as the
   * empty type.
   *
   * @param declaringTypes The types whose constructor this could be
   * @param parameters Potential parameters of the constructor. Each set represents a possibility of
   *     parameter types at that position
   * @return The constructor definition
   */
  public static UnsolvedMethodAlternates createConstructor(
      List<UnsolvedClassOrInterfaceAlternates> declaringTypes, List<Set<MemberType>> parameters) {
    return withConstructorsMarked(
        create(
            declaringTypes.get(0).getClassName(),
            Set.of(new SolvedMemberType("")),
            declaringTypes,
            parameters));
  }

  /**
   * Creates a new unsolved constructor declaration, recording nodes that must be preserved if a
   * given parameter type is chosen. See {@link #createConstructor} for why there is no name
   * parameter.
   *
   * @param declaringTypes The types whose constructor this could be
   * @param parameters Potential parameters of the constructor. Each map represents a possibility of
   *     parameter types at that position, along with nodes that must be preserved if that type is
   *     chosen
   * @return The constructor definition
   */
  public static UnsolvedMethodAlternates createConstructorWithPreservation(
      List<UnsolvedClassOrInterfaceAlternates> declaringTypes,
      List<Map<MemberType, @Nullable Node>> parameters) {
    return withConstructorsMarked(
        createWithPreservation(
            declaringTypes.get(0).getClassName(),
            Set.of(new SolvedMemberType("")),
            declaringTypes,
            parameters,
            List.of()));
  }

  /**
   * Replaces every alternate of the given method with a copy marked as a constructor. The create
   * methods build ordinary methods; this is how the constructor factories above turn their result
   * into constructors.
   *
   * @param method the method whose alternates should be marked
   * @return {@code method}, for chaining
   */
  private static UnsolvedMethodAlternates withConstructorsMarked(UnsolvedMethodAlternates method) {
    List<UnsolvedMethod> alternates = method.getAlternates();

    for (int i = 0; i < alternates.size(); i++) {
      UnsolvedMethod alternate = alternates.get(i);

      alternates.set(
          i,
          new UnsolvedMethod(
              alternate.getName(),
              alternate.getReturnType(),
              alternate.getParameterList(),
              alternate.getThrownExceptions(),
              alternate.getMustPreserveNodes(),
              alternate.getAccessModifier(),
              alternate.isStatic(),
              alternate.getTypeVariableNames(),
              true));
    }

    return method;
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
                  old.getTypeVariableNames(),
                  old.isConstructor());
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

  @Override
  public Set<String> getFullyQualifiedNames() {
    Set<String> fqns = new LinkedHashSet<>();

    for (UnsolvedMethod methodAlternate : getAlternates()) {
      // The name is prepended below, once per declaring type: a constructor takes its name from
      // the type that declares it, so it is not the same for every key built here.
      StringBuilder methodSignature = new StringBuilder("(");

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
          // A constructor is named for the type that declares it (JLS 8.8.1), so every alternate
          // declaring type contributes a key under its own simple name.
          String name =
              methodAlternate.isConstructor()
                  ? QualifiedTypeName.parse(fqn).simpleName()
                  : methodAlternate.getName();

          fqns.add(fqn + "#" + name + methodSignature);
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

  @Override
  public String getTypeVariableName(int index) {
    return getAlternates().get(0).getTypeVariableName(index);
  }

  @Override
  public void declareTypeVariables(List<String> names) {
    applyToAllAlternates(UnsolvedMethod::declareTypeVariables, names);
  }

  /**
   * Returns whether the given type is one of the type variables bound by this method's own
   * declaration (i.e. a name introduced by this method, such as the {@code T} in {@code <T> T
   * get()}).
   *
   * <p>Such a name is only meaningful inside the declaration that binds it. Callers that are about
   * to use a type outside of that declaration -- for example, to describe the type of an expression
   * at a call site -- must not use the name, because it is not in scope there.
   *
   * @param type The type to check
   * @return true if the type is a type variable bound by this method
   */
  public boolean isOwnTypeVariable(MemberType type) {
    Set<String> fqns = type.getFullyQualifiedNames();
    if (fqns.size() != 1 || !type.getTypeArguments().isEmpty()) {
      return false;
    }

    String name = fqns.iterator().next();
    for (UnsolvedMethod alternate : getAlternates()) {
      for (int i = 0; i < alternate.getNumberOfTypeVariables(); i++) {
        if (alternate.getTypeVariableName(i).equals(name)) {
          return true;
        }
      }
    }
    return false;
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
   * Gets the parameter list, where each index contains a set of all possible parameter types at
   * that index.
   *
   * @return The parameter list
   */
  public List<Set<MemberType>> getParameterList() {
    List<Set<MemberType>> parameterList = new ArrayList<>();

    for (UnsolvedMethod alternate : getAlternates()) {
      List<MemberType> parameters = alternate.getParameterList();

      for (int i = 0; i < parameters.size(); i++) {
        MemberType param = parameters.get(i);

        if (parameterList.size() <= i) {
          parameterList.add(new LinkedHashSet<>());
        }

        parameterList.get(i).add(param);
      }
    }

    return parameterList;
  }

  @Override
  public String getName() {
    return getAlternates().get(0).getName();
  }

  @Override
  public boolean isConstructor() {
    return getAlternates().get(0).isConstructor();
  }

  @Override
  public List<MemberType> getThrownExceptions() {
    return getAlternates().get(0).getThrownExceptions();
  }

  @Override
  public void addThrownException(MemberType exception) {
    applyToAllAlternates(UnsolvedMethod::addThrownException, exception);
  }

  /**
   * Records that this method might declare the given exception in its throws clause, by adding a
   * copy of each existing alternate with the exception added. Use this instead of {@link
   * #addThrownException} when it is not certain that this method is the one that throws the
   * exception.
   *
   * <p>Note that {@link UnsolvedMethod#equals} ignores the throws clause, so a later call to {@link
   * #removeDuplicateAlternates()} collapses the alternates added here back into whichever of the
   * two possibilities comes first.
   *
   * @param exception the exception that this method might throw
   * @param preferred whether the alternates that declare the exception should be preferred; if
   *     true, they are placed before the alternates that do not declare it
   */
  public void addAlternatesWithThrownException(MemberType exception, boolean preferred) {
    List<UnsolvedMethod> throwing = new ArrayList<>();

    for (UnsolvedMethod alternate : getAlternates()) {
      if (alternate.getThrownExceptions().contains(exception)) {
        // This method is already known to throw the exception; there is no alternative to record.
        return;
      }

      UnsolvedMethod copy =
          new UnsolvedMethod(
              alternate.getName(),
              alternate.getReturnType(),
              alternate.getParameterList(),
              alternate.getThrownExceptions(),
              alternate.getMustPreserveNodes(),
              alternate.getAccessModifier(),
              alternate.isStatic(),
              alternate.getTypeVariableNames(),
              alternate.isConstructor());
      copy.addThrownException(exception);
      throwing.add(copy);
    }

    if (preferred) {
      getAlternates().addAll(0, throwing);
    } else {
      getAlternates().addAll(throwing);
    }
  }

  /**
   * Use with caution: this method sets all alternates' return types to the same type.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public void setReturnType(MemberType memberType) {
    applyToAllAlternates(UnsolvedMethod::setReturnType, memberType);
    removeDuplicateAlternates();
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
                alternate.getTypeVariableNames(),
                alternate.isConstructor());
        addAlternate(newAlternate);
      }

      seenParameterLists.add(alternate.getParameterList());
    }
  }

  /**
   * Replaces a parameter type with new parameter types in all alternates.
   *
   * @param oldType The parameter type to replace
   * @param newTypes The parameter types to replace with
   */
  public void replaceParameterType(MemberType oldType, Set<MemberType> newTypes) {
    int originalSize = getAlternates().size();
    for (int i = 0; i < originalSize; i++) {
      UnsolvedMethod alternate = getAlternates().get(i);

      List<MemberType> parameterList = alternate.getParameterList();
      boolean hasOldType = parameterList.contains(oldType);

      if (hasOldType) {
        boolean isFirst = true;
        for (MemberType newType : newTypes) {
          if (isFirst) {
            alternate.replaceParameterType(oldType, newType);
            isFirst = false;
            continue;
          }

          List<MemberType> newParameterList =
              parameterList.stream().map(param -> param.equals(oldType) ? newType : param).toList();
          UnsolvedMethod newAlternate =
              new UnsolvedMethod(
                  alternate.getName(),
                  alternate.getReturnType(),
                  newParameterList,
                  alternate.getThrownExceptions(),
                  alternate.getMustPreserveNodes(),
                  alternate.getAccessModifier(),
                  alternate.isStatic(),
                  alternate.getTypeVariableNames(),
                  alternate.isConstructor());

          addAlternate(newAlternate);
        }
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
  public void setContent(String content) {
    applyToAllAlternates(UnsolvedMethod::setContent, content);
  }

  @Override
  public boolean isStatic() {
    return getAlternates().get(0).isStatic();
  }
}
