package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.body.CallableDeclaration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Given a name, field type, and a set of potential encapsulating classes, this class allows for
 * alternates of a same field to be generated in different locations. If a class were:
 *
 * <pre><code>
 * class A extends B implements C {
 *    void x() {
 *      int y = a;
 *    }
 * }
 * </code></pre>
 *
 * where B and C are both unresolvable, field a could be in either one. Note that {@code
 * getAlternates()} will always return a single alternate for this class, since {@code
 * UnsolvedFieldAlternates} depends on encapsulating classes for alternate definitions.
 */
public class UnsolvedFieldAlternates extends UnsolvedSymbolAlternates<UnsolvedField>
    implements UnsolvedFieldCommon {
  private UnsolvedFieldAlternates(
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes) {
    super(alternateDeclaringTypes);
  }

  /**
   * Creates a new instance of a field. Note that there is only one alternate generated here, but
   * there could potentially be many different declaring types.
   *
   * @param name The name of the field
   * @param typesToMustPreserveNodes A map of field types to must-preserve nodes. Different field
   *     types may lead to different sets of nodes that need to be conditionally preserved.
   * @param alternateDeclaringTypes Potential declaring types
   * @param isStatic Whether the field is static or not
   * @param isFinal Whether the field is final or not
   * @return The generated field
   */
  public static UnsolvedFieldAlternates create(
      String name,
      Map<MemberType, CallableDeclaration<?>> typesToMustPreserveNodes,
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes,
      boolean isStatic,
      boolean isFinal) {
    if (alternateDeclaringTypes.isEmpty()) {
      throw new RuntimeException("Unsolved field must have at least one potential declaring type.");
    }

    UnsolvedFieldAlternates result = new UnsolvedFieldAlternates(alternateDeclaringTypes);

    for (Map.Entry<MemberType, CallableDeclaration<?>> entry :
        typesToMustPreserveNodes.entrySet()) {
      UnsolvedField field =
          new UnsolvedField(name, entry.getKey(), isStatic, isFinal, Set.of(entry.getValue()));
      result.addAlternate(field);
    }

    return result;
  }

  /**
   * Creates a new instance of a field. Note that there is only one alternate generated here, but
   * there could potentially be many different declaring types.
   *
   * @param name The name of the field
   * @param type The type of the field
   * @param alternateDeclaringTypes Potential declaring types
   * @param isStatic Whether the field is static or not
   * @param isFinal Whether the field is final or not
   * @return The generated field
   */
  public static UnsolvedFieldAlternates create(
      String name,
      MemberType type,
      List<UnsolvedClassOrInterfaceAlternates> alternateDeclaringTypes,
      boolean isStatic,
      boolean isFinal) {
    if (alternateDeclaringTypes.isEmpty()) {
      throw new RuntimeException("Unsolved field must have at least one potential declaring type.");
    }

    UnsolvedFieldAlternates result = new UnsolvedFieldAlternates(alternateDeclaringTypes);

    UnsolvedField field = new UnsolvedField(name, type, isStatic, isFinal, Set.of());
    result.addAlternate(field);

    return result;
  }

  /**
   * Updates field types and must preserve nodes. Saves the intersection of the previous and the
   * input, since we know more information to narrow potential field types down.
   *
   * @param typesToPreserveNodes A map of field types to nodes that must be preserved
   */
  public void updateFieldTypesAndMustPreserveNodes(
      Map<MemberType, CallableDeclaration<?>> typesToPreserveNodes) {
    // Update in-place; intersection = removing all elements in the original set
    // that isn't found in the updated set
    UnsolvedField old = getAlternates().get(0);
    List<MemberType> oldFieldTypes = getTypes();
    getAlternates().removeIf(alternate -> !typesToPreserveNodes.containsKey(alternate.getType()));

    if (getAlternates().isEmpty() && oldFieldTypes.size() == 1) {
      // If it's now empty and old field types was of size 1, it was probably a synthetic field
      // type
      for (Map.Entry<MemberType, CallableDeclaration<?>> entry : typesToPreserveNodes.entrySet()) {
        UnsolvedField field =
            new UnsolvedField(
                old.getName(),
                entry.getKey(),
                old.isStatic(),
                old.isFinal(),
                Set.of(entry.getValue()));
        addAlternate(field);
      }
    }
  }

  @Override
  public Set<String> getFullyQualifiedNames() {
    Set<String> fqns = new LinkedHashSet<>();

    for (UnsolvedClassOrInterfaceAlternates alternate : getAlternateDeclaringTypes()) {
      for (String fqn : alternate.getFullyQualifiedNames()) {
        fqns.add(fqn + "#" + getAlternates().get(0).getName());
      }
    }

    return fqns;
  }

  /**
   * Gets the field types.
   *
   * @return The field types
   */
  public List<MemberType> getTypes() {
    return getAlternates().stream().map(alternate -> alternate.getType()).toList();
  }

  public void replaceFieldType(MemberType oldType, MemberType newType) {
    for (UnsolvedField alternate : getAlternates()) {
      if (alternate.getType().equals(oldType)) {
        alternate.setType(newType);
      }
    }
  }

  @Override
  public String getName() {
    return getAlternates().get(0).getName();
  }

  @Override
  public boolean isStatic() {
    return doAllAlternatesReturnTrueFor(UnsolvedField::isStatic);
  }

  @Override
  public boolean isFinal() {
    return doAllAlternatesReturnTrueFor(UnsolvedField::isFinal);
  }
}
