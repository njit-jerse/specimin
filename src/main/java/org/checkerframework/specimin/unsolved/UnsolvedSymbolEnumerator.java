package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.Node;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.specimin.JavaParserUtil;
import org.checkerframework.specimin.QualifiedTypeName;
import org.checkerframework.specimin.Slicer;

/**
 * Enumerates possible combinations of unsolved symbols, given a set of generated unsolved symbols
 * from the {@link Slicer}. Depending on the (TODO: ambiguity resolution policy), this class may
 * enumerate one to all possibilities.
 */
public class UnsolvedSymbolEnumerator {
  /** The unsolved types that must be included in the output. */
  private final Set<UnsolvedClassOrInterfaceAlternates> unsolvedTypes = new LinkedHashSet<>();

  /** The unsolved fields that must be included in the output. */
  private final Set<UnsolvedFieldAlternates> unsolvedFields = new LinkedHashSet<>();

  /** The unsolved methods and constructors that must be included in the output. */
  private final Set<UnsolvedCallableAlternates<?>> unsolvedMethods = new LinkedHashSet<>();

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
        if (unsolvedSymbol.getAlternateDeclaringTypes().isEmpty()) {
          continue;
        }

        unsolvedFields.add(field);
      } else if (unsolvedSymbol instanceof UnsolvedCallableAlternates<?> callable) {
        if (unsolvedSymbol.getAlternateDeclaringTypes().isEmpty()) {
          continue;
        }

        unsolvedMethods.add(callable);
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
    // will be an inner type; Foo will also be a key, Bar will be an inner type. However, outerTypes
    // will only contain Foo.
    Map<UnsolvedClassOrInterface, Set<UnsolvedClassOrInterface>> outerTypesToInnerTypes =
        new LinkedHashMap<>();

    for (UnsolvedClassOrInterfaceAlternates unsolved : unsolvedTypes) {
      addTypeToCorrectDataStructure(unsolved, outerTypes, outerTypesToInnerTypes);
      for (MemberType implemented : unsolved.getAlternates().get(0).getImplementedTypes()) {
        addAllUsedTypesToSet(implemented, outerTypes, outerTypesToInnerTypes);
      }

      MemberType extended = unsolved.getAlternates().get(0).getExtendedType();
      if (extended != null) {
        addAllUsedTypesToSet(extended, outerTypes, outerTypesToInnerTypes);
      }
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

    Map<UnsolvedClassOrInterface, Set<UnsolvedCallable>> typesToMethods = new LinkedHashMap<>();

    for (UnsolvedCallableAlternates<?> unsolved : unsolvedMethods) {
      UnsolvedCallable method = unsolved.getAlternates().get(0);
      UnsolvedClassOrInterfaceAlternates typeAlternates =
          unsolved.getAlternateDeclaringTypes().get(0);
      UnsolvedClassOrInterface type = typeAlternates.getAlternates().get(0);
      if (!typesToMethods.containsKey(type)) {
        typesToMethods.put(type, new LinkedHashSet<>());

        addTypeToCorrectDataStructure(typeAlternates, outerTypes, outerTypesToInnerTypes);
      }

      typesToMethods.get(type).add(method);

      // Only a method has a return type (JLS 8.8.1).
      if (method instanceof UnsolvedMethod asMethod) {
        addAllUsedTypesToSet(asMethod.getReturnType(), outerTypes, outerTypesToInnerTypes);
      }

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
      Map<UnsolvedClassOrInterface, Set<UnsolvedCallable>> typesToMethods,
      Map<UnsolvedClassOrInterface, Set<UnsolvedClassOrInterface>> outerTypesToInnerTypes,
      Set<Node> ableToRemove,
      boolean isInnerClass) {
    Set<UnsolvedField> fields = typesToFields.get(type);

    if (fields == null) {
      fields = Set.of();
    }

    Set<UnsolvedCallable> methods = typesToMethods.get(type);

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

    for (UnsolvedCallable method : methods) {
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

    // Alternate declaring types may not be empty but the first alternate could still be an outer
    // type. This could happen when Foo is not imported, so Foo could either be located in the
    // unsolved parent class or in the same package.
    if (type.getAlternateDeclaringTypes().isEmpty()
        || (JavaParserUtil.isAClassPath(alternate.getFullyQualifiedName())
            && JavaParserUtil.isProbablyAPackage(
                alternate
                    .getFullyQualifiedName()
                    .substring(0, alternate.getFullyQualifiedName().lastIndexOf('.'))))) {
      outerTypes.add(alternate);
    } else {
      for (UnsolvedClassOrInterfaceAlternates declaringType : type.getAlternateDeclaringTypes()) {
        outerTypesToInnerTypes
            .computeIfAbsent(declaringType.getAlternates().get(0), k -> new LinkedHashSet<>())
            .add(alternate);
      }

      // A nested type is emitted only inside its enclosing type's declaration, so filing it above
      // is not enough: the type that encloses it has to be placed as well, and it may be reachable
      // by no other route. A single-type-import declaration may name a nested type (JLS 7.5.1), in
      // which case the enclosing type is never written in the input at all.
      //
      // Only the enclosure of the alternate this best effort chose is placed. The other alternate
      // declaring types are enclosures of the alternates it did not choose, and emitting those
      // would emit whole types that nothing in the output refers to.
      UnsolvedClassOrInterfaceAlternates enclosing = chosenEnclosingType(type);

      if (enclosing != null) {
        addTypeToCorrectDataStructure(enclosing, outerTypes, outerTypesToInnerTypes);
      }
    }
  }

  /**
   * Returns the alternate declaring type that encloses the alternate of {@code type} that this best
   * effort chose, i.e. the one whose fully-qualified name is that alternate's qualifier.
   *
   * @param type a type that has at least one alternate declaring type
   * @return the enclosing type of the chosen alternate, or null if no alternate declaring type is
   *     that alternate's qualifier
   */
  private @Nullable UnsolvedClassOrInterfaceAlternates chosenEnclosingType(
      UnsolvedClassOrInterfaceAlternates type) {
    QualifiedTypeName enclosingName =
        QualifiedTypeName.parse(type.getAlternates().get(0).getFullyQualifiedName())
            .enclosingName();

    if (enclosingName == null) {
      return null;
    }

    for (UnsolvedClassOrInterfaceAlternates declaringType : type.getAlternateDeclaringTypes()) {
      if (declaringType.getFullyQualifiedNames().contains(enclosingName.toString())) {
        return declaringType;
      }
    }

    return null;
  }
}
