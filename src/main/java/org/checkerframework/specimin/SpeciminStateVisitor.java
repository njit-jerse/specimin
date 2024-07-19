package org.checkerframework.specimin;

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
import com.google.common.base.Splitter;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;

/**
 * This visitor contains shared logic and state for the Specimin's various XVisitor classes. It
 * should not be used directly.
 *
 * <p>This class tracks the following: the lists of target methods and fields, the lists of used
 * members and classes, and the set of existing classes to file paths. It may be expanded to handle
 * additional state tracking in the future.
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
   * This boolean tracks whether the element currently being visited is inside a target method or
   * field.
   */
  protected boolean insideTargetMember = false;

  /** The simple name of the class currently visited */
  protected @ClassGetSimpleName String className = "";

  /** The qualified name of the class currently being visited. */
  protected String currentClassQualifiedName = "";

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
   * Given a method declaration, this method return the declaration of that method without the
   * return type and any possible annotation.
   *
   * @param methodDeclaration the method declaration to be used as input
   * @return methodDeclaration without the return type and any possible annotation.
   */
  public static String removeMethodReturnTypeAndAnnotations(String methodDeclaration) {
    String methodDeclarationWithoutParen =
        methodDeclaration.substring(0, methodDeclaration.indexOf("("));
    List<String> methodParts = Splitter.onPattern(" ").splitToList(methodDeclarationWithoutParen);
    String methodName = methodParts.get(methodParts.size() - 1);
    String methodReturnType = methodDeclaration.substring(0, methodDeclaration.indexOf(methodName));
    String methodWithoutReturnType = methodDeclaration.replace(methodReturnType, "");
    methodParts = Splitter.onPattern(" ").splitToList(methodWithoutReturnType);
    String filteredMethodDeclaration =
        methodParts.stream()
            .filter(part -> !part.startsWith("@"))
            .map(part -> part.indexOf('@') == -1 ? part : part.substring(0, part.indexOf('@')))
            .collect(Collectors.joining(" "));
    // sometimes an extra space may occur if an annotation right after a < was removed
    return filteredMethodDeclaration.replace("< ", "<");
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
    result.append(
        removeMethodReturnTypeAndAnnotations(decl.getDeclarationAsString(false, false, false)));
    return result.toString().replaceAll("\\s", "");
  }

  @Override
  public Visitable visit(FieldDeclaration node, Void p) {
    for (VariableDeclarator var : node.getVariables()) {
      boolean oldInsideTargetMember = insideTargetMember;
      insideTargetMember =
          targetFields.contains(currentClassQualifiedName + "#" + var.getNameAsString());
      super.visit(var, p);
      insideTargetMember = oldInsideTargetMember;
    }
    // Do not call super, since sub-nodes are already visited by the call to super.visit
    // on the declarators above.
    return node;
  }

  @Override
  public Visitable visit(MethodDeclaration methodDeclaration, Void p) {
    boolean oldInsideTargetMember = insideTargetMember;
    String methodQualifiedSignature = getSignature(methodDeclaration);
    insideTargetMember = targetMethods.contains(methodQualifiedSignature);
    Visitable result = super.visit(methodDeclaration, p);
    insideTargetMember = oldInsideTargetMember;
    return result;
  }

  @Override
  public Visitable visit(ConstructorDeclaration ctorDecl, Void p) {
    String methodQualifiedSignature = getSignature(ctorDecl);
    boolean oldInsideTargetMember = insideTargetMember;
    if (targetMethods.contains(methodQualifiedSignature)) {
      insideTargetMember = true;
    }
    Visitable result = super.visit(ctorDecl, p);
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
}
