package org.checkerframework.specimin;

import com.github.javaparser.ast.Node;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.checkerframework.specimin.unsolved.MemberType;
import org.checkerframework.specimin.unsolved.UnsolvedClassOrInterface;
import org.checkerframework.specimin.unsolved.UnsolvedClassOrInterfaceAlternates;
import org.checkerframework.specimin.unsolved.UnsolvedField;
import org.checkerframework.specimin.unsolved.UnsolvedFieldAlternates;
import org.checkerframework.specimin.unsolved.UnsolvedMethod;
import org.checkerframework.specimin.unsolved.UnsolvedMethodAlternates;
import org.checkerframework.specimin.unsolved.UnsolvedSymbolAlternates;

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
    Set<UnsolvedClassOrInterface> types = new LinkedHashSet<>();

    for (UnsolvedClassOrInterfaceAlternates unsolved : unsolvedTypes) {
      types.add(unsolved.getAlternates().get(0));
    }

    Map<UnsolvedClassOrInterface, Set<UnsolvedField>> typesToFields = new LinkedHashMap<>();

    for (UnsolvedFieldAlternates unsolved : unsolvedFields) {
      UnsolvedField field = unsolved.getAlternates().get(0);
      UnsolvedClassOrInterface type =
          unsolved.getAlternateDeclaringTypes().get(0).getAlternates().get(0);
      if (!typesToFields.containsKey(type)) {
        typesToFields.put(type, new LinkedHashSet<>());
        types.add(type);
      }

      typesToFields.get(type).add(field);

      MemberType typeOfField = field.getType();
      if (typeOfField.isUnsolved()) {
        types.add(typeOfField.getUnsolvedType().getAlternates().get(0));
      }
    }

    Map<UnsolvedClassOrInterface, Set<UnsolvedMethod>> typesToMethods = new LinkedHashMap<>();

    for (UnsolvedMethodAlternates unsolved : unsolvedMethods) {
      UnsolvedMethod method = unsolved.getAlternates().get(0);
      UnsolvedClassOrInterface type =
          unsolved.getAlternateDeclaringTypes().get(0).getAlternates().get(0);
      if (!typesToMethods.containsKey(type)) {
        typesToMethods.put(type, new LinkedHashSet<>());
        types.add(type);
      }

      typesToMethods.get(type).add(method);

      MemberType typesToMethod = method.getReturnType();
      if (typesToMethod.isUnsolved()) {
        types.add(typesToMethod.getUnsolvedType().getAlternates().get(0));
      }

      for (MemberType parameterType : method.getParameterList()) {
        if (parameterType.isUnsolved()) {
          types.add(parameterType.getUnsolvedType().getAlternates().get(0));
        }
      }
    }

    Map<String, String> result = new LinkedHashMap<>();

    Set<Node> ableToRemove = new HashSet<>(allDependentNodes);

    for (UnsolvedClassOrInterface type : types) {
      Set<UnsolvedField> fields = typesToFields.get(type);

      if (fields == null) {
        fields = Set.of();
      }

      Set<UnsolvedMethod> methods = typesToMethods.get(type);

      if (methods == null) {
        methods = Set.of();
      }

      ableToRemove.removeAll(type.getMustPreserveNodes());

      for (UnsolvedField field : fields) {
        ableToRemove.removeAll(field.getMustPreserveNodes());
      }

      for (UnsolvedMethod method : methods) {
        ableToRemove.removeAll(method.getMustPreserveNodes());
      }

      result.put(
          type.getFullyQualifiedName(),
          type.toString(methods, fields, Collections.emptyList(), false));
    }

    return new UnsolvedSymbolEnumeratorResult(result, ableToRemove);
  }
}
