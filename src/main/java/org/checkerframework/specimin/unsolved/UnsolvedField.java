package org.checkerframework.specimin.unsolved;

public class UnsolvedField {
  /** The name of the field */
  private final String name;

  /** The type of the field. */
  private final MemberType type;

  /** This is set to true if this field is a static field */
  private final boolean isStatic = false;

  /** This is set to true if this field is a final field */
  private final boolean isFinal = false;

  /**
   * Create an instance of UnsolvedField.
   *
   * @param name the name of the field
   * @param type the type of the field
   * @param isStatic if the field is static
   * @param isFinal if the field is final
   */
  public UnsolvedField(String name, MemberType type, boolean isStatic, boolean isFinal) {
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
  public MemberType getType() {
    return type;
  }

  /**
   * Get the name of this field
   *
   * @return the name of this field
   */
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    String typeAsString;

    if (type.isUnsolved()) {
      typeAsString = type.getUnsolvedType().toString();
      throw new RuntimeException("You haven't implemented this part yet");
    } else {
      typeAsString = type.getSolvedType();
    }

    return "public "
        + (isStatic ? "static " : "")
        + (isFinal ? "final " : "")
        + typeAsString
        + " "
        + name
        + ";";
  }
}
