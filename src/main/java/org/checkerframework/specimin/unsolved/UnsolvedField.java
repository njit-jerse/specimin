package org.checkerframework.specimin.unsolved;

import com.github.javaparser.ast.Node;
import java.util.Objects;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.specimin.JavaParserUtil;

/** Represents a single unsolved field alternate. */
public class UnsolvedField extends UnsolvedSymbolAlternate implements UnsolvedFieldCommon {
  /** The name of the field */
  private final String name;

  /** The type of the field. */
  private MemberType type;

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
   * @param mustPreserveNodes the nodes that must be preserved
   */
  public UnsolvedField(
      String name,
      MemberType type,
      boolean isStatic,
      boolean isFinal,
      Set<Node> mustPreserveNodes) {
    super(mustPreserveNodes);
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
   * Sets the type of this field.
   *
   * @param type the new type to set
   */
  public void setType(MemberType type) {
    this.type = type;
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

  /**
   * Return the content of this field. If declaringTypeType is ENUM, then this is also formatted as
   * an enum constant declaration.
   *
   * @param declaringTypeType The type of the declaring type
   * @return The field declaration
   */
  public String toString(UnsolvedClassOrInterfaceType declaringTypeType) {
    if (declaringTypeType == UnsolvedClassOrInterfaceType.ENUM) {
      // Still compilable even with an extra comma at the end
      return name + ",";
    }
    return "public "
        + (isStatic ? "static " : "")
        + (isFinal ? "final " : "")
        + type
        + " "
        + name
        + (isStatic && isFinal ? " = " + JavaParserUtil.getInitializerRHS(type.toString()) : "")
        + ";";
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (!(o instanceof UnsolvedField other)) {
      return false;
    }
    return other.name.equals(this.name)
        && other.type.equals(this.type)
        && other.isStatic == this.isStatic
        && other.isFinal == this.isFinal;
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, isStatic, isFinal);
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  @Override
  public boolean isFinal() {
    return isFinal;
  }
}
