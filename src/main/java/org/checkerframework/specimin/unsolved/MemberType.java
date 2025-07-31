package org.checkerframework.specimin.unsolved;

import java.util.Set;

/**
 * Class to represent the type of an unsolved field, the return type of an unsolved method, or the
 * types of an unsolved method's parameters.
 *
 * <p>Use this class instead of hardcoding a string into {@link UnsolvedMethod} or {@link
 * UnsolvedField} to ensure proper types when alternates are generated.
 */
public interface MemberType {
  /**
   * Gets the set of fully qualified names for this type.
   *
   * @return The set of fully qualified names representing this type
   */
  public Set<String> getFullyQualifiedNames();
}
