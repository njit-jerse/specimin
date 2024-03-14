package org.checkerframework.specimin;

import com.github.javaparser.ast.body.MethodDeclaration;
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
    } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
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
    ResolvedMethodDeclaration resolvedSuperCall =
        MustImplementMethodsVisitor.getOverriddenMethodInSuperClass(methodDeclaration);
    if (resolvedSuperCall != null) {
      usedClass.add(resolvedSuperCall.getPackageName() + "." + resolvedSuperCall.getClassName());
      usedMembers.add(resolvedSuperCall.getQualifiedSignature());
    }
  }
}
