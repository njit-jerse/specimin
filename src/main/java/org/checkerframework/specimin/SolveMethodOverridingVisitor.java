package org.checkerframework.specimin;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;

/**
 * If used or target methods override another methods, this visitor updates the list of used classes
 * and methods accordingly.
 */
public class SolveMethodOverridingVisitor extends SpeciminStateVisitor {

  /**
   * Constructs a new SolveMethodOverridingVisitor with the provided sets of target methods, used
   * members, and used classes.
   *
   * @param previousVisitor the last visitor to run before this one
   */
  public SolveMethodOverridingVisitor(SpeciminStateVisitor previousVisitor) {
    super(previousVisitor);
  }

  @Override
  public Visitable visit(MethodDeclaration method, Void p) {
    String methodSignature;
    try {
      methodSignature = method.resolve().getQualifiedSignature();
    } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
      // this method is not used by target methods, so it is unresolved.
      return super.visit(method, p);
    }
    if (targetMethods.contains(methodSignature) || usedMembers.contains(methodSignature)) {
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
    ResolvedMethodDeclaration resolvedSuperCall =
        MustImplementMethodsVisitor.getOverriddenMethodInSuperClass(methodDeclaration);
    if (resolvedSuperCall != null) {
      usedTypeElements.add(
          resolvedSuperCall.getPackageName() + "." + resolvedSuperCall.getClassName());
      usedMembers.add(resolvedSuperCall.getQualifiedSignature());
    }
  }
}
