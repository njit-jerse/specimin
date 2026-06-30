package org.checkerframework.specimin.unsolved;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.checkerframework.specimin.JavaLangUtils;
import org.checkerframework.specimin.JavaParserUtil;

public class SpeciminGenerationUtils {
  private SpeciminGenerationUtils() {
    // Utility class, do not instantiate
  }

  /**
   * Get a map of different member types between two otherwise-similar MemberTypes. For example, for
   * List<Integer> and List<String>, return a map with the entry Integer -> String. For Foo<List<?
   * extends String>, Integer> and Foo<List<? extends Number>, Integer>, return a map with the entry
   * String --> Number.
   *
   * @param type1 The first type
   * @param type2 The second type
   * @return A map of different member types between the two types
   */
  public static Map<MemberType, MemberType> getDifferentMemberTypes(
      MemberType type1, MemberType type2) {
    return getDifferentMemberTypesImpl(type1, type2, new HashMap<>());
  }

  /**
   * Equivalent to {@link #getDifferentMemberTypes(MemberType, MemberType)}, but with comparison
   * between a MemberType and FullyQualifiedNameSet.
   *
   * @param type1 The first type
   * @param type2 The second type
   * @return A map of different types between the two types
   */
  public static Map<MemberType, FullyQualifiedNameSet> getDifferentTypes(
      MemberType type1, FullyQualifiedNameSet type2) {
    return getDifferentTypesImpl(type1, type2, new HashMap<>());
  }

  /**
   * Determine if a MemberType is a type variable. This is a heuristic, but should match most, if
   * not all cases.
   *
   * @param memberType The MemberType to check
   * @return True if the MemberType is a type variable, false otherwise
   */
  public static boolean isATypeVariable(MemberType memberType) {
    if (memberType.getFullyQualifiedNames().size() != 1) {
      return false;
    }

    String name = memberType.getFullyQualifiedNames().iterator().next();
    String simple = JavaParserUtil.getSimpleNameFromQualifiedName(JavaParserUtil.erase(name));

    return name.equals(simple)
        && memberType.getTypeArguments().isEmpty()
        && !JavaLangUtils.isPrimitive(simple);
  }

  /**
   * Copies a MemberType and replaces any occurrences of toReplace with the new type, including in
   * wildcard bounds and type arguments. If no changes were made, the original instance is returned.
   *
   * @param original The original MemberType to copy
   * @param toReplace The MemberType to replace
   * @param replaceWith The MemberType to replace with
   * @return A new MemberType with the replacement applied
   */
  public static MemberType copyTypeWithReplacedMemberType(
      MemberType original, MemberType toReplace, MemberType replaceWith) {
    if (original.equals(toReplace)) {
      return replaceWith;
    }

    if (original instanceof WildcardMemberType wildcard) {
      MemberType bound = wildcard.getBound();
      if (bound == null) {
        return original;
      }
      MemberType newBound = copyTypeWithReplacedMemberType(bound, toReplace, replaceWith);
      if (newBound.equals(bound)) {
        return original;
      }
      return new WildcardMemberType(newBound, wildcard.isUpperBounded());
    }

    boolean replaced = false;
    List<MemberType> newArgs = new ArrayList<>();
    for (MemberType arg : original.getTypeArguments()) {
      MemberType newArg = copyTypeWithReplacedMemberType(arg, toReplace, replaceWith);
      if (!newArg.equals(arg)) {
        replaced = true;
      }
      newArgs.add(newArg);
    }

    if (!replaced) {
      return original;
    }

    return original.copyWithNewTypeArgs(newArgs);
  }

  /**
   * Helper method for getDifferentMemberTypes. Recursively compares two MemberTypes and populates
   * the differences map with any differing types.
   *
   * @param type1 The first MemberType to compare
   * @param type2 The second MemberType to compare
   * @param differences A map to populate with differing MemberTypes
   * @return A map of differing MemberTypes between type1 and type2
   */
  private static Map<MemberType, MemberType> getDifferentMemberTypesImpl(
      MemberType type1, MemberType type2, Map<MemberType, MemberType> differences) {
    if (!type1.getFullyQualifiedNames().equals(type2.getFullyQualifiedNames())) {
      if (type1 instanceof WildcardMemberType wildcard1
          && type2 instanceof WildcardMemberType wildcard2) {
        MemberType bound1 = wildcard1.getBound();
        MemberType bound2 = wildcard2.getBound();

        if (!Objects.equals(bound1, bound2)) {
          differences.put(type1, type2);
          return differences;
        }

        if (bound1 == null || bound2 == null) {
          return differences;
        }

        return getDifferentMemberTypesImpl(bound1, bound2, differences);
      }

      differences.put(type1, type2);
      return differences;
    }

    if (type1.getTypeArguments().size() != type2.getTypeArguments().size()) {
      differences.put(type1, type2);
      return differences;
    }

    for (int i = 0; i < type1.getTypeArguments().size(); i++) {
      MemberType arg1 = type1.getTypeArguments().get(i);
      MemberType arg2 = type2.getTypeArguments().get(i);
      getDifferentMemberTypesImpl(arg1, arg2, differences);
    }

    return differences;
  }

  /**
   * Helper method for getDifferentTypes.
   *
   * @param type1 The MemberType to compare
   * @param type2 The FullyQualifiedNameSet to compare
   * @param differences A map to populate with differing types
   * @return A map of differing types between type1 and type2
   */
  private static Map<MemberType, FullyQualifiedNameSet> getDifferentTypesImpl(
      MemberType type1,
      FullyQualifiedNameSet type2,
      Map<MemberType, FullyQualifiedNameSet> differences) {
    if (!type1.getFullyQualifiedNames().equals(type2.erasedFqns())) {
      if (type1 instanceof WildcardMemberType wildcard1 && type2.wildcard() != null) {
        MemberType bound1 = wildcard1.getBound();

        if (bound1 == null || type2.equals(FullyQualifiedNameSet.UNBOUNDED_WILDCARD)) {
          if (bound1 == null && type2.equals(FullyQualifiedNameSet.UNBOUNDED_WILDCARD)) {
            differences.put(type1, type2);
          }
          return differences;
        }

        return getDifferentTypesImpl(
            bound1,
            new FullyQualifiedNameSet(type2.erasedFqns(), type2.typeArguments()),
            differences);
      }

      differences.put(type1, type2);
      return differences;
    }

    if (type1.getTypeArguments().size() != type2.typeArguments().size()) {
      differences.put(type1, type2);
      return differences;
    }

    for (int i = 0; i < type1.getTypeArguments().size(); i++) {
      getDifferentTypesImpl(
          type1.getTypeArguments().get(i), type2.typeArguments().get(i), differences);
    }

    return differences;
  }
}
