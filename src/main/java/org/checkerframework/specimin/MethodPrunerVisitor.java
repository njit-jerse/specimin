package org.checkerframework.specimin;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import java.util.Set;

/**
 * This visitor removes every method in the compilation unit that is not a member of its {@link
 * #methodsToLeaveUnchanged} set or {@link #methodsToEmpty} set. It also removes the bodies of all
 * methods in the {@link #methodsToEmpty} set and replaces their bodies with "throw new Error();".
 */
public class MethodPrunerVisitor extends ModifierVisitor<Void> {

  /** The methods that should NOT be touched by this pruner. */
  private Set<ResolvedMethodDeclaration> methodsToLeaveUnchanged;

  /** The methods whose bodies should be pruned. */
  private Set<ResolvedMethodDeclaration> methodsToEmpty;

  /**
   * Creates the pruner. All methods this pruner encounters other than those in its input sets will
   * be removed entirely.
   *
   * @param methodsToEmpty the set of methods whose bodies should be removed
   * @param methodsToKeep the set of methods whose bodies should be kept intact (usually the target
   *     methods for specimin)
   */
  public MethodPrunerVisitor(
      Set<ResolvedMethodDeclaration> methodsToEmpty, Set<ResolvedMethodDeclaration> methodsToKeep) {
    this.methodsToEmpty = methodsToEmpty;
    this.methodsToLeaveUnchanged = methodsToKeep;
  }

  @Override
  public Visitable visit(MethodDeclaration methodDecl, Void p) {
    ResolvedMethodDeclaration resolved = methodDecl.resolve();
    if (methodsToLeaveUnchanged.contains(resolved)) {
      return super.visit(methodDecl, p);
    } else if (methodsToEmpty.contains(resolved)) {
      methodDecl.removeBody();
      return methodDecl;
    } else {
      methodDecl.remove();
      return methodDecl;
    }
  }
}
