package org.checkerframework.specimin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
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
   * @return A map of class names to file content
   */
  public Map<String, String> getBestEffort() {
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
      }

      typesToFields.get(type).add(field);
    }

    Map<UnsolvedClassOrInterface, Set<UnsolvedMethod>> typesToMethods = new LinkedHashMap<>();

    for (UnsolvedMethodAlternates unsolved : unsolvedMethods) {
      UnsolvedMethod field = unsolved.getAlternates().get(0);
      UnsolvedClassOrInterface type =
          unsolved.getAlternateDeclaringTypes().get(0).getAlternates().get(0);
      if (!typesToMethods.containsKey(type)) {
        typesToMethods.put(type, new LinkedHashSet<>());
      }

      typesToMethods.get(type).add(field);
    }

    Map<String, String> result = new LinkedHashMap<>();

    for (UnsolvedClassOrInterface type : types) {
      Set<UnsolvedField> fields = typesToFields.get(type);

      if (fields == null) {
        fields = Set.of();
      }

      Set<UnsolvedMethod> methods = typesToMethods.get(type);

      if (methods == null) {
        methods = Set.of();
      }

      result.put(
          type.getFullyQualifiedName(),
          type.toString(methods, fields, Collections.emptyList(), false));
    }

    return result;
  }
}
