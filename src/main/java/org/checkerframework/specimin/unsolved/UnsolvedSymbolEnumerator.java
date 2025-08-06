package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.Node;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.checkerframework.specimin.AmbiguityResolutionPolicy;
import org.checkerframework.specimin.Slicer;

/**
 * Enumerates possible combinations of unsolved symbols, given a set of generated unsolved symbols
 * from the {@link Slicer}. Depending on the {@link AmbiguityResolutionPolicy}, this class may
 * enumerate one to all possibilities.
 */
public class UnsolvedSymbolEnumerator {
  private final Set<UnsolvedClassOrInterfaceAlternates> unsolvedTypes = new LinkedHashSet<>();
  private final Set<UnsolvedFieldAlternates> unsolvedFields = new LinkedHashSet<>();
  private final Set<UnsolvedMethodAlternates> unsolvedMethods = new LinkedHashSet<>();

  /**
   * Creates a new instance of UnsolvedSymbolEnumerator.
   *
   * @param unsolvedSlice The slice of generated unsolved symbols, from the {@link Slicer}.
   */
  public UnsolvedSymbolEnumerator(Set<UnsolvedSymbolAlternates<?>> unsolvedSlice) {
    for (UnsolvedSymbolAlternates<?> unsolvedSymbol : unsolvedSlice) {
      if (unsolvedSymbol instanceof UnsolvedClassOrInterfaceAlternates type) {
        unsolvedTypes.add(type);
      } else if (unsolvedSymbol instanceof UnsolvedFieldAlternates field) {
        unsolvedFields.add(field);
      } else if (unsolvedSymbol instanceof UnsolvedMethodAlternates method) {
        unsolvedMethods.add(method);
      }
    }
  }

  /**
   * Gets the best effort unsolved symbol generation.
   *
   * @param allDependentNodes The set of all nodes that are dependent on some alternate
   * @return A map of class names to file content
   */
  public UnsolvedSymbolEnumeratorResult getBestEffort(Set<Node> allDependentNodes) {
    // Best effort is the first alternate in every alternate set
    // This set should not contain any inner classes.
    Set<UnsolvedClassOrInterface> outerTypes = new LinkedHashSet<>();

    // Note that the keyset is not equal to outerTypes. For Foo.Bar.Baz, Bar will be a key here, Baz
    // will be an inner type;
    // Foo will also be a key, Bar will be an inner type. However, outerTypes will only contain Foo.
    Map<UnsolvedClassOrInterface, Set<UnsolvedClassOrInterface>> outerTypesToInnerTypes =
        new LinkedHashMap<>();

    for (UnsolvedClassOrInterfaceAlternates unsolved : unsolvedTypes) {
      addTypeToCorrectDataStructure(unsolved, outerTypes, outerTypesToInnerTypes);
    }

    Map<UnsolvedClassOrInterface, Set<UnsolvedField>> typesToFields = new LinkedHashMap<>();

    for (UnsolvedFieldAlternates unsolved : unsolvedFields) {
      UnsolvedField field = unsolved.getAlternates().get(0);
      UnsolvedClassOrInterfaceAlternates typeAlternates =
          unsolved.getAlternateDeclaringTypes().get(0);
      UnsolvedClassOrInterface type = typeAlternates.getAlternates().get(0);
      if (!typesToFields.containsKey(type)) {
        typesToFields.put(type, new LinkedHashSet<>());

        addTypeToCorrectDataStructure(typeAlternates, outerTypes, outerTypesToInnerTypes);
      }

      typesToFields.get(type).add(field);

      addAllUsedTypesToSet(field.getType(), outerTypes, outerTypesToInnerTypes);
    }

    Map<UnsolvedClassOrInterface, Set<UnsolvedMethod>> typesToMethods = new LinkedHashMap<>();

    for (UnsolvedMethodAlternates unsolved : unsolvedMethods) {
      UnsolvedMethod method = unsolved.getAlternates().get(0);
      UnsolvedClassOrInterfaceAlternates typeAlternates =
          unsolved.getAlternateDeclaringTypes().get(0);
      UnsolvedClassOrInterface type = typeAlternates.getAlternates().get(0);
      if (!typesToMethods.containsKey(type)) {
        typesToMethods.put(type, new LinkedHashSet<>());

        addTypeToCorrectDataStructure(typeAlternates, outerTypes, outerTypesToInnerTypes);
      }

      typesToMethods.get(type).add(method);

      addAllUsedTypesToSet(method.getReturnType(), outerTypes, outerTypesToInnerTypes);

      for (MemberType parameterType : method.getParameterList()) {
        addAllUsedTypesToSet(parameterType, outerTypes, outerTypesToInnerTypes);
      }
    }

    Map<String, String> result = new LinkedHashMap<>();

    Set<Node> ableToRemove = new HashSet<>(allDependentNodes);

    for (UnsolvedClassOrInterface type : outerTypes) {
      result.put(
          type.getFullyQualifiedName(),
          getTypeDeclarationAsString(
              type, typesToFields, typesToMethods, outerTypesToInnerTypes, ableToRemove, false));
    }

    return new UnsolvedSymbolEnumeratorResult(result, ableToRemove);
  }

  /**
   * Gets the type declaration as a string, including all fields and methods. Also modifies the
   * ableToRemove set by side effect.
   *
   * @param type The type to get the declaration for
   * @param typesToFields A map of types to their fields
   * @param typesToMethods A map of types to their methods
   * @param outerTypesToInnerTypes A map of outer types to their inner types
   * @param ableToRemove The set of nodes that can be removed in this iteration
   * @param isInnerClass Whether the type is an inner class
   * @return The type declaration as a string
   */
  private String getTypeDeclarationAsString(
      UnsolvedClassOrInterface type,
      Map<UnsolvedClassOrInterface, Set<UnsolvedField>> typesToFields,
      Map<UnsolvedClassOrInterface, Set<UnsolvedMethod>> typesToMethods,
      Map<UnsolvedClassOrInterface, Set<UnsolvedClassOrInterface>> outerTypesToInnerTypes,
      Set<Node> ableToRemove,
      boolean isInnerClass) {
    Set<UnsolvedField> fields = typesToFields.get(type);

    if (fields == null) {
      fields = Set.of();
    }

    Set<UnsolvedMethod> methods = typesToMethods.get(type);

    if (methods == null) {
      methods = Set.of();
    }

    Set<UnsolvedClassOrInterface> innerTypes = outerTypesToInnerTypes.get(type);

    if (innerTypes == null) {
      innerTypes = Set.of();
    }

    ableToRemove.removeAll(type.getMustPreserveNodes());

    for (UnsolvedField field : fields) {
      ableToRemove.removeAll(field.getMustPreserveNodes());
    }

    for (UnsolvedMethod method : methods) {
      ableToRemove.removeAll(method.getMustPreserveNodes());
    }

    return type.toString(
        methods,
        fields,
        innerTypes.stream()
            .map(
                inner ->
                    getTypeDeclarationAsString(
                        inner,
                        typesToFields,
                        typesToMethods,
                        outerTypesToInnerTypes,
                        ableToRemove,
                        true))
            .toList(),
        isInnerClass);
  }

  /**
   * Given a MemberType, recursively adds all used UnsolvedClassOrInterface types to the correct
   * data structure by calling {@link #addTypeToCorrectDataStructure}.
   *
   * @param memberType The member type
   * @param types The set to add to
   */
  private void addAllUsedTypesToSet(
      MemberType memberType,
      Set<UnsolvedClassOrInterface> types,
      Map<UnsolvedClassOrInterface, Set<UnsolvedClassOrInterface>> outerTypesToInnerTypes) {
    for (MemberType typeArg : memberType.getTypeArguments()) {
      addAllUsedTypesToSet(typeArg, types, outerTypesToInnerTypes);
    }

    if (memberType instanceof UnsolvedMemberType unsolvedType) {
      addTypeToCorrectDataStructure(unsolvedType.getUnsolvedType(), types, outerTypesToInnerTypes);
    } else if (memberType instanceof WildcardMemberType wildcardType) {
      MemberType bound = wildcardType.getBound();

      if (bound != null) {
        addAllUsedTypesToSet(bound, types, outerTypesToInnerTypes);
      }
    }
  }

  /**
   * If an UnsolvedClassOrInterfaceAlternates type is an inner class, it is added to the
   * outerTypesToInnerTypes map. If it is an outer class, it is added to the outerTypes set.
   *
   * @param type The type
   * @param outerTypes The set of outer types
   * @param outerTypesToInnerTypes The map of outer types to their inner types
   */
  private void addTypeToCorrectDataStructure(
      UnsolvedClassOrInterfaceAlternates type,
      Set<UnsolvedClassOrInterface> outerTypes,
      Map<UnsolvedClassOrInterface, Set<UnsolvedClassOrInterface>> outerTypesToInnerTypes) {
    UnsolvedClassOrInterface alternate = type.getAlternates().get(0);

    if (type.getAlternateDeclaringTypes().isEmpty()) {
      outerTypes.add(alternate);
    } else {
      for (UnsolvedClassOrInterfaceAlternates declaringType : type.getAlternateDeclaringTypes()) {
        outerTypesToInnerTypes
            .computeIfAbsent(declaringType.getAlternates().get(0), k -> new LinkedHashSet<>())
            .add(alternate);
      }
    }
  }
}
