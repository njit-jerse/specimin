package org.checkerframework.specimin.unsolved;

/** Represents the type that UnsolvedClassOrInterface represents. */
public enum UnsolvedClassOrInterfaceType {
  /** Represents an unknown type; could be any value in this enum. */
  UNKNOWN,
  /** Represents a class type. */
  CLASS,
  /** Represents an interface type. */
  INTERFACE,
  /** Represents an annotation type. */
  ANNOTATION,
  /** Represents an enum type. */
  ENUM
}
