package org.checkerframework.specimin;

import com.github.javaparser.ast.visitor.ModifierVisitor;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * This visitor contains shared logic and state for the Specimin's various XVisitor classes. It
 * should not be used directly.
 *
 * <p>This class tracks the following: - the lists of target methods and fields - the lists of used
 * members and classes - the set of existing classes to file paths
 */
public /*abstract*/ class SpeciminStateVisitor extends ModifierVisitor<Void> {

  /** Set containing the signatures of target methods. */
  protected final Set<String> targetMethods;

  /**
   * The members (methods and fields) that were actually used by the targets, and therefore ought to
   * have their specifications (but not bodies) preserved. The Strings in the set are the
   * fully-qualified names, as returned by ResolvedMethodDeclaration#getQualifiedSignature for
   * methods and FieldAccessExpr#getName for fields.
   */
  protected final Set<String> usedMembers;

  /**
   * Type elements (classes, interfaces, and enums) related to the methods used by the targets.
   * These classes will be included in the input.
   */
  protected final Set<String> usedTypeElement;

  /** for checking if class files are in the original codebase. */
  protected final Map<String, Path> existingClassesToFilePath;

  /**
   * Constructs a new instance with the provided sets. Use this constructor only for the first
   * visitor to run.
   *
   * @param usedMembers Set containing the signatures of used members.
   * @param usedTypeElement Set containing the signatures of used classes.
   * @param existingClassesToFilePath map from existing classes to file paths
   */
  public SpeciminStateVisitor(
      Set<String> targetMethods,
      Set<String> usedMembers,
      Set<String> usedTypeElement,
      Map<String, Path> existingClassesToFilePath) {
    this.targetMethods = targetMethods;
    this.usedMembers = usedMembers;
    this.usedTypeElement = usedTypeElement;
    this.existingClassesToFilePath = existingClassesToFilePath;
  }

  /**
   * Constructor that copies state from the previous visitor. All state remains mutable (it's a
   * shallow copy).
   *
   * @param previous the previous visitor to run
   */
  public SpeciminStateVisitor(SpeciminStateVisitor previous) {
    this.targetMethods = previous.targetMethods;
    this.usedTypeElement = previous.usedTypeElement;
    this.usedMembers = previous.usedMembers;
    this.existingClassesToFilePath = previous.existingClassesToFilePath;
  }

  /**
   * Get the set containing the signatures of used members.
   *
   * @return The set containing the signatures of used members.
   */
  public Set<String> getUsedMembers() {
    return usedMembers;
  }

  /**
   * Get the set containing the signatures of used classes.
   *
   * @return The set containing the signatures of used classes.
   */
  public Set<String> getUsedTypeElements() {
    return usedTypeElement;
  }
}
