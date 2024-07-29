package org.checkerframework.specimin;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithDeclaration;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;
import org.checkerframework.specimin.modularity.ModularityModel;

/**
 * This visitor contains shared logic and state for the Specimin's various XVisitor classes. It
 * should not be used directly.
 *
 * <p>This class tracks the following: the lists of target methods and fields, the lists of used
 * members and classes, and the set of existing classes to file paths. It may be expanded to handle
 * additional state tracking in the future.
 */
public abstract class SpeciminStateVisitor extends ModifierVisitor<Void> {

  /** The modularity model currently in use. */
  protected final ModularityModel modularityModel;

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
   * This boolean tracks whether the element currently being visited is inside a target method or
   * field.
   */
  protected boolean insideTargetMember = false;

  /**
   * Is the visitor inside a target constructor? If this boolean is true, then {@link
   * #insideTargetMember} is also guaranteed to be true.
   */
  protected boolean insideTargetCtor = false;

  /** The simple name of the class currently visited */
  protected @ClassGetSimpleName String className = "";

  /** The qualified name of the class currently being visited. */
  protected String currentClassQualifiedName = "";

  /**
   * The fully-qualified names of each field that is assigned by a target constructor. The
   * assignments to these fields will be preserved, so Specimin needs to avoid adding an initializer
   * for them if they are final (as it does for other, non-assigned-by-target final fields). Set by
   * {@link TargetMemberFinderVisitor} but stored here so that it is easily available later when
   * pruning.
   */
  protected final Set<String> fieldsAssignedByTargetCtors;

  /**
   * Constructs a new instance with the provided sets. Use this constructor only for the first
   * visitor to run.
   *
   * @param targetMethods the fully-qualified signatures of the target methods, in the form returned
   *     by ResolvedMethodDeclaration#getQualifiedSignature but optionally containing spaces between
   *     parameters, which this constructor guarantees will be removed
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
      ModularityModel model,
      Map<String, Path> existingClassesToFilePath) {
    this.targetMethods = new HashSet<>();
    for (String methodSignature : targetMethods) {
      // remove spaces
      this.targetMethods.add(methodSignature.replaceAll("\\s", ""));
    }
    this.targetFields = targetFields;
    this.usedMembers = usedMembers;
    this.usedTypeElements = usedTypeElements;
    this.existingClassesToFilePath = existingClassesToFilePath;
    this.fieldsAssignedByTargetCtors = new HashSet<>();
    this.modularityModel = model;
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
    this.insideTargetMember = previous.insideTargetMember;
    this.className = previous.className;
    this.currentClassQualifiedName = previous.currentClassQualifiedName;
    this.fieldsAssignedByTargetCtors = previous.fieldsAssignedByTargetCtors;
    this.modularityModel = previous.modularityModel;
  }

  /**
   * Get the set containing the signatures of used classes.
   *
   * @return The set containing the signatures of used classes.
   */
  public Set<String> getUsedTypeElements() {
    return usedTypeElements;
  }

  /**
   * Gets the (fully-qualified) signature of a declaration (method or constructor). Removes things
   * like annotations, the return type, spaces, etc.
   *
   * @param decl a method or constructor declaration
   * @return the fully qualified signature of that declaration
   */
  protected String getSignature(NodeWithDeclaration decl) {
    StringBuilder result = new StringBuilder();
    result.append(this.currentClassQualifiedName);
    result.append("#");
    result.append(JavaParserUtil.removeMethodReturnTypeSpacesAndAnnotations(decl));
    return result.toString();
  }

  @Override
  public Visitable visit(VariableDeclarator var, Void p) {
    boolean oldInsideTargetMember = insideTargetMember;
    insideTargetMember =
        oldInsideTargetMember
            || targetFields.contains(currentClassQualifiedName + "#" + var.getNameAsString());
    Visitable result = super.visit(var, p);
    insideTargetMember = oldInsideTargetMember;
    return result;
  }

  @Override
  public Visitable visit(MethodDeclaration methodDeclaration, Void p) {
    boolean oldInsideTargetMember = insideTargetMember;
    String methodQualifiedSignature = getSignature(methodDeclaration);
    insideTargetMember = oldInsideTargetMember || targetMethods.contains(methodQualifiedSignature);
    Visitable result = super.visit(methodDeclaration, p);
    insideTargetMember = oldInsideTargetMember;
    return result;
  }

  @Override
  public Visitable visit(ConstructorDeclaration ctorDecl, Void p) {
    String methodQualifiedSignature = getSignature(ctorDecl);
    boolean oldInsideTargetMember = insideTargetMember;
    insideTargetMember = oldInsideTargetMember || targetMethods.contains(methodQualifiedSignature);
    boolean oldInsideTargetCtor = insideTargetCtor;
    insideTargetCtor = oldInsideTargetCtor || targetMethods.contains(methodQualifiedSignature);
    Visitable result = super.visit(ctorDecl, p);
    insideTargetCtor = oldInsideTargetCtor;
    insideTargetMember = oldInsideTargetMember;
    return result;
  }

  @Override
  public Visitable visit(EnumDeclaration node, Void arg) {
    maintainDataStructuresPreSuper(node);
    Visitable result = super.visit(node, arg);
    maintainDataStructuresPostSuper(node);
    return result;
  }

  @Override
  public Visitable visit(ClassOrInterfaceDeclaration node, Void arg) {
    maintainDataStructuresPreSuper(node);
    Visitable result = super.visit(node, arg);
    maintainDataStructuresPostSuper(node);
    return result;
  }

  /**
   * Maintains the data structures of this class (like the {@link #className}, {@link
   * #currentClassQualifiedName}, etc.) based on a class, interface, or enum declaration. Call this
   * method before calling super.visit().
   *
   * @param decl the class, interface, or enum declaration
   */
  protected void maintainDataStructuresPreSuper(TypeDeclaration<?> decl) {
    SimpleName nodeName = decl.getName();
    className = nodeName.asString();
    if (decl.isNestedType()) {
      this.currentClassQualifiedName += "." + decl.getName().asString();
    } else if (!JavaParserUtil.isLocalClassDecl(decl)) {
      // the purpose of keeping track of class name is to recognize the signatures of target
      // methods. Since we don't support methods inside local classes as target methods, we don't
      // need
      // to keep track of class name in this case.
      this.currentClassQualifiedName = decl.getFullyQualifiedName().orElseThrow();
    }
  }

  /**
   * Maintains the data structures of this class (like the {@link #className}, {@link
   * #currentClassQualifiedName}, etc.) based on a class, interface, or enum declaration. Call this
   * method after calling super.visit().
   *
   * @param decl the class, interface, or enum declaration
   */
  protected void maintainDataStructuresPostSuper(TypeDeclaration<?> decl) {
    if (decl.isNestedType()) {
      this.currentClassQualifiedName =
          this.currentClassQualifiedName.substring(
              0, this.currentClassQualifiedName.lastIndexOf('.'));
    } else if (!JavaParserUtil.isLocalClassDecl(decl)) {
      this.currentClassQualifiedName = "";
    }
  }

  /**
   * Determines if the given Node is a target/used member or class.
   *
   * @param node The node to check
   * @return true iff the given Node is a target/used member or type.
   */
  protected boolean isTargetOrUsed(Node node) {
    String qualifiedName;
    boolean isClass = false;
    if (node instanceof ClassOrInterfaceDeclaration) {
      Optional<String> qualifiedNameOptional =
          ((ClassOrInterfaceDeclaration) node).getFullyQualifiedName();
      if (qualifiedNameOptional.isEmpty()) {
        return false;
      }
      qualifiedName = qualifiedNameOptional.get();
      isClass = true;
    } else if (node instanceof EnumDeclaration) {
      Optional<String> qualifiedNameOptional = ((EnumDeclaration) node).getFullyQualifiedName();
      if (qualifiedNameOptional.isEmpty()) {
        return false;
      }
      qualifiedName = qualifiedNameOptional.get();
      isClass = true;
    } else if (node instanceof AnnotationDeclaration) {
      Optional<String> qualifiedNameOptional =
          ((AnnotationDeclaration) node).getFullyQualifiedName();
      if (qualifiedNameOptional.isEmpty()) {
        return false;
      }
      qualifiedName = qualifiedNameOptional.get();
      isClass = true;
    } else if (node instanceof ConstructorDeclaration) {
      try {
        qualifiedName = ((ConstructorDeclaration) node).resolve().getQualifiedSignature();
      } catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
        // UnsupportedOperationException: type is a type variable
        // See TargetMethodFinderVisitor.visit(MethodDeclaration, Void) for more details
        return false;
      } catch (RuntimeException e) {
        // The current class is employed by the target methods, although not all of its members are
        // utilized. It's not surprising for unused members to remain unresolved.
        // If this constructor is from the parent of the current class, and it is not resolved, we
        // will get a RuntimeException, otherwise just a UnsolvedSymbolException.
        // Copied from PrunerVisitor.visit(ConstructorDeclaration, Void)
        return false;
      }
    } else if (node instanceof MethodDeclaration) {
      try {
        qualifiedName = ((MethodDeclaration) node).resolve().getQualifiedSignature();
      } catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
        // UnsupportedOperationException: type is a type variable
        // See TargetMethodFinderVisitor.visit(MethodDeclaration, Void) for more details
        return false;
      }
    } else if (node instanceof FieldDeclaration) {
      try {
        FieldDeclaration decl = (FieldDeclaration) node;
        for (VariableDeclarator var : decl.getVariables()) {
          qualifiedName = JavaParserUtil.getEnclosingClassName(decl) + "#" + var.getNameAsString();
          if (usedMembers.contains(qualifiedName) || targetFields.contains(qualifiedName)) {
            return true;
          }
        }
      } catch (UnsolvedSymbolException ex) {
        return false;
      }
      return false;
    } else {
      return false;
    }

    if (qualifiedName.contains(" ")) {
      qualifiedName = qualifiedName.replaceAll("//s", "");
    }

    if (isClass) {
      return usedTypeElements.contains(qualifiedName);
    } else {
      // fields should already be handled at this point
      return usedMembers.contains(qualifiedName) || targetMethods.contains(qualifiedName);
    }
  }
}
