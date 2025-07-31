package org.checkerframework.specimin.unsolved;

import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

public class UnsolvedField extends UnsolvedSymbolAlternate implements UnsolvedFieldCommon {
  /** The name of the field */
  private final String name;

  /** The type of the field. */
  private final MemberType type;

  /** This is set to true if this field is a static field */
  private boolean isStatic = false;

  /** This is set to true if this field is a final field */
  private boolean isFinal = false;

  /**
   * Create an instance of UnsolvedField.
   *
   * @param name the name of the field
   * @param type the type of the field
   * @param isStatic if the field is static
   * @param isFinal if the field is final
   */
  public UnsolvedField(String name, MemberType type, boolean isStatic, boolean isFinal) {
    // Haven't found a case yet where nodes need to be selectively preserved for fields
    super(Set.of());
    this.name = name;
    this.type = type;
    this.isStatic = isStatic;
    this.isFinal = isFinal;
  }

  /**
   * Get the type of this field
   *
   * @return the value of type
   */
  @Override
  public MemberType getType() {
    return type;
  }

  /**
   * Get the name of this field
   *
   * @return the name of this field
   */
  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "public "
        + (isStatic ? "static " : "")
        + (isFinal ? "final " : "")
        + type
        + " "
        + name
        + ";";
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (!(o instanceof UnsolvedField other)) {
      return false;
    }
    return other.name.equals(this.name);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }
}
