package org.checkerframework.specimin;

import com.github.javaparser.ast.visitor.ModifierVisitor;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * This visitor contains shared logic and state for the Specimin's various XVisitor classes. It
 * should not be used directly.
 *
 * <p>This class tracks the following: the lists of target methods and fields, the lists of used
 * members and classes, and the set of existing classes to file paths. It may be expanded to
 * handle additional state tracking in the future.
 */
public abstract class SpeciminStateVisitor extends ModifierVisitor<Void> {

  /**
   * Set containing the signatures of target methods. The Strings in the set are the fully-qualified
   * names, as returned by ResolvedMethodDeclaration#getQualifiedSignature.
   */
  protected final Set<String> targetMethods;

  /**
   * Set containing the fully-qualified names of target fields. The format is
   * class.fully.qualified.Name#fieldName.
   */
  protected final Set<String> targetFields;

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
  protected final Set<String> usedTypeElements;

  /** for checking if class files are in the original codebase. */
  protected final Map<String, Path> existingClassesToFilePath;

  /**
   * Constructs a new instance with the provided sets. Use this constructor only for the first
   * visitor to run.
   *
   * @param targetMethods the fully-qualified signatures of the target methods, in the form returned
   *     by ResolvedMethodDeclaration#getQualifiedSignature
   * @param targetFields the fully-qualified names of the target fields, in the form
   *     class.fully.qualified.Name#fieldName
   * @param usedMembers set containing the signatures of used members
   * @param usedTypeElements set containing the signatures of used classes, enums, annotations, etc.
   * @param existingClassesToFilePath map from existing classes to file paths
   */
  public SpeciminStateVisitor(
      Set<String> targetMethods,
      Set<String> targetFields,
      Set<String> usedMembers,
      Set<String> usedTypeElements,
      Map<String, Path> existingClassesToFilePath) {
    this.targetMethods = targetMethods;
    this.targetFields = targetFields;
    this.usedMembers = usedMembers;
    this.usedTypeElements = usedTypeElements;
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
    this.targetFields = previous.targetFields;
    this.usedTypeElements = previous.usedTypeElements;
    this.usedMembers = previous.usedMembers;
    this.existingClassesToFilePath = previous.existingClassesToFilePath;
  }

  /**
   * Get the set containing the signatures of used classes.
   *
   * @return The set containing the signatures of used classes.
   */
  public Set<String> getUsedTypeElements() {
    return usedTypeElements;
  }
}
