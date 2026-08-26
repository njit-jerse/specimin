package org.checkerframework.specimin;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;

/**
 * A type name decomposed into its identifiers: {@code library.Outer.Nested} becomes {@code
 * [library, Outer, Nested]}. Specifically, this class can answer questions about:
 *
 * <ul>
 *   <li>the <b>first identifier</b> is the one a single-type-import must be matched against (JLS
 *       6.5.5.2 resolves {@code Outer.Nested} by first resolving {@code Outer}, so it is {@code
 *       Outer} that {@code import library.Outer;} binds), and
 *   <li>the <b>simple name</b> is the last identifier (JLS 6.2), which is the name a constructor
 *       declaration must use (JLS 8.8.1) and the name the type is declared under.
 * </ul>
 *
 * <p>Deciding where a name's package part ends and its type part begins does <em>not</em> have an
 * exact answer in the general case, and is not handled by this class: JLS 6.5.2 asks whether
 * package {@code Q} contains a type {@code Id}, which needs a classpath that Specimin may not have.
 * Naming convention logic is used to guess the answer to that question via {@link
 * JavaParserUtil#isProbablyAPackage(String)} and {@link JavaParserUtil#isAClassPath(String)}.
 *
 * <p>Prefer this class' AST factories over {@link #parse(String)}: a name that is still attached to
 * source carries its own structure, so reading it needs no string surgery. Type arguments and array
 * brackets are not part of a type's name and are dropped by every factory.
 */
public final class QualifiedTypeName {
  /** The identifiers of this name, leftmost first. Never empty. */
  private final List<String> identifiers;

  /**
   * Creates a new QualifiedTypeName. Private; use the factory methods.
   *
   * @param identifiers the identifiers of the name, leftmost first; must not be empty
   */
  private QualifiedTypeName(List<String> identifiers) {
    if (identifiers.isEmpty()) {
      throw new IllegalArgumentException("A type name must have at least one identifier.");
    }
    this.identifiers = Collections.unmodifiableList(new ArrayList<>(identifiers));
  }

  /**
   * Returns the decomposition of a type name as it is written in source.
   *
   * @param type the type
   * @return the decomposition of {@code type}'s name
   */
  public static QualifiedTypeName of(ClassOrInterfaceType type) {
    List<String> identifiers = new ArrayList<>();

    for (ClassOrInterfaceType part = type; part != null; part = part.getScope().orElse(null)) {
      identifiers.add(part.getNameAsString());
    }

    Collections.reverse(identifiers);
    return new QualifiedTypeName(identifiers);
  }

  /**
   * Returns the decomposition of a name node, such as the name of an annotation.
   *
   * @param name the name
   * @return the decomposition of {@code name}
   */
  public static QualifiedTypeName of(Name name) {
    List<String> identifiers = new ArrayList<>();

    for (Name part = name; part != null; part = part.getQualifier().orElse(null)) {
      identifiers.add(part.getIdentifier());
    }

    Collections.reverse(identifiers);
    return new QualifiedTypeName(identifiers);
  }

  /**
   * Returns the decomposition of an expression that names a type. Every shape a type name can take
   * in an expression position is accepted: a {@code TypeExpr} (which is how JavaParser wraps the
   * scope of a method reference, whether or not that scope really is a type -- see {@link
   * JavaParserUtil#getMethodRefScopeAsVariable}), and the {@code FieldAccessExpr} chain bottoming
   * out in a {@code NameExpr} that a qualified name takes anywhere else.
   *
   * <p>Callers that reach this method by way of a naming-convention guess, such as {@link
   * JavaParserUtil#isAClassPath(String)}, are still asking an exact question of it: given that this
   * name denotes a type, what are its identifiers?
   *
   * @param expr the expression
   * @return the decomposition of the name {@code expr} writes, or null if {@code expr} is not a
   *     name
   */
  public static @Nullable QualifiedTypeName ofTypeName(Expression expr) {
    if (expr.isTypeExpr()) {
      return expr.asTypeExpr().getType().isClassOrInterfaceType()
          ? of(expr.asTypeExpr().getType().asClassOrInterfaceType())
          : null;
    }

    List<String> identifiers = new ArrayList<>();

    for (Expression part = expr; ; ) {
      if (part.isFieldAccessExpr()) {
        identifiers.add(part.asFieldAccessExpr().getNameAsString());
        part = part.asFieldAccessExpr().getScope();
      } else if (part.isNameExpr()) {
        identifiers.add(part.asNameExpr().getNameAsString());
        break;
      } else {
        return null;
      }
    }

    Collections.reverse(identifiers);
    return new QualifiedTypeName(identifiers);
  }

  /**
   * Returns the decomposition of a dotted name. Use an AST factory instead wherever the name is
   * still attached to source.
   *
   * @param dotted a type name, such as a fully-qualified name; may carry type arguments or array
   *     brackets, which are dropped
   * @return the decomposition of {@code dotted}
   */
  public static QualifiedTypeName parse(String dotted) {
    String name = JavaParserUtil.removeArrayBrackets(JavaParserUtil.erase(dotted));

    return new QualifiedTypeName(List.of(name.split("\\.", -1)));
  }

  /**
   * Returns the leftmost identifier of this name. This is the identifier that names either the
   * outermost enclosing type or the first segment of the package, and so is the one that an import
   * declaration can bind (JLS 6.5.5.2).
   *
   * @return the leftmost identifier
   */
  public String firstIdentifier() {
    return identifiers.get(0);
  }

  /**
   * Returns the simple name of the type this name denotes: its rightmost identifier (JLS 6.2).
   *
   * @return the simple name
   */
  @SuppressWarnings("signature") // an identifier of a type name is that type's simple name
  public @ClassGetSimpleName String simpleName() {
    return identifiers.get(identifiers.size() - 1);
  }

  /**
   * Returns this name without its last identifier: the name of the package or of the enclosing type
   * that qualifies it. Which of those two it is cannot be decided here.
   *
   * @return the qualifier, or null if this name is unqualified
   */
  public @Nullable QualifiedTypeName enclosingName() {
    return isQualified()
        ? new QualifiedTypeName(identifiers.subList(0, identifiers.size() - 1))
        : null;
  }

  /**
   * Returns whether this name is qualified, i.e. whether it has more than one identifier.
   *
   * @return true if this name has a qualifier
   */
  public boolean isQualified() {
    return identifiers.size() > 1;
  }

  /**
   * Returns the identifiers of this name, leftmost first. The list is read-only and never empty.
   *
   * @return the identifiers
   */
  public List<String> identifiers() {
    return identifiers;
  }

  @Override
  public String toString() {
    return String.join(".", identifiers);
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return other instanceof QualifiedTypeName otherName
        && identifiers.equals(otherName.identifiers);
  }

  @Override
  public int hashCode() {
    return identifiers.hashCode();
  }
}
