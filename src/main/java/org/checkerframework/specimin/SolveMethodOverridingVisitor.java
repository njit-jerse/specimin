package org.checkerframework.specimin;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import java.util.Set;

/**
 * If used or target methods override another methods, this visitor updates the list of used classes
 * and methods accordingly.
 */
public class SolveMethodOverridingVisitor extends ModifierVisitor<Void> {
  /** Set containing the signatures of target methods. */
  private Set<String> targetMethod;

  /** Set containing the signatures of used member (fields and methods). */
  private Set<String> usedMembers;

  /** Set containing the signatures of used classes. */
  private Set<String> usedClass;

  /**
   * Constructs a new SolveMethodOverridingVisitor with the provided sets of target methods, used
   * members, and used classes.
   *
   * @param targetMethod Set containing the signatures of target methods.
   * @param usedMembers Set containing the signatures of used members.
   * @param usedClass Set containing the signatures of used classes.
   */
  public SolveMethodOverridingVisitor(
      Set<String> targetMethod, Set<String> usedMembers, Set<String> usedClass) {
    this.targetMethod = targetMethod;
    this.usedMembers = usedMembers;
    this.usedClass = usedClass;
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
  public Set<String> getUsedClass() {
    return usedClass;
  }

  @Override
  public Visitable visit(MethodDeclaration method, Void p) {
    String methodSignature;
    try {
      methodSignature = method.resolve().getQualifiedSignature();
    } catch (UnsolvedSymbolException e) {
      // this method is not used by target methods, so it is unresolved.
      return super.visit(method, p);
    }
    if (targetMethod.contains(methodSignature) || usedMembers.contains(methodSignature)) {
      checkForOverridingAndUpdateUsedClasses(method);
    }
    return super.visit(method, p);
  }

  /**
   * Given a MethodDeclaration, this method checks whether that method declaration overrides another
   * method. If it does, it updates the list of used classes and members accordingly. Note:
   * methodDeclaration is assumed to be a target or used method.
   */
  private void checkForOverridingAndUpdateUsedClasses(MethodDeclaration methodDeclaration) {
    // just a method signature, no need to check for overriding.
    if (methodDeclaration.getBody().isEmpty()) {
      return;
    }
    BlockStmt methodBody = methodDeclaration.getBody().get();
    // JavaParser does not support solving overriding, but it does support solving super
    // expressions. So we make a temporary super expression to figure out if this current method is
    // overriding.
    MethodCallExpr superCall = new MethodCallExpr();
    superCall.setName(methodDeclaration.getName());
    NodeList<Parameter> parameters = methodDeclaration.getParameters();
    for (Parameter parameter : parameters) {
      superCall.addArgument(parameter.getNameAsString());
    }
    superCall.setScope(new SuperExpr());
    methodBody.addStatement(superCall);
    try {
      ResolvedMethodDeclaration resolvedSuperCall = superCall.resolve();
      usedClass.add(resolvedSuperCall.getPackageName() + "." + resolvedSuperCall.getClassName());
      usedMembers.add(resolvedSuperCall.getQualifiedSignature());
    } catch (Exception e) {
      // The current method is not overriding, thus the super call is unresolved.
      // This catch block is necessary to avoid crashes due to ignored catch blocks. A single
      // remove() call is not enough to remove a MethodCallExpr.
      superCall.remove();
    }
    removeNode(superCall);
  }

  /**
   * Removes a node from its compilation unit. If a node cannot be removed directly, it might be
   * wrapped inside another node, causing removal failure. This method iterates through the parent
   * nodes until it successfully removes the specified node.
   *
   * <p>If this explanation does not make sense to you, please refer to the following link for
   * further details: <a
   * href="https://github.com/javaparser/javaparser/issues/858">https://github.com/javaparser/javaparser/issues/858</a>
   *
   * @param node The node to be removed.
   */
  private void removeNode(Node node) {
    while (!node.remove()) {
      node = node.getParentNode().get();
    }
  }
}
