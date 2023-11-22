package org.checkerframework.specimin;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import java.util.Set;

/**
 * This visitor removes every method in the compilation unit that is not a member of its {@link
 * #methodsToLeaveUnchanged} set or {@link #methodsToEmpty} set. It also removes the bodies of all
 * methods in the {@link #methodsToEmpty} set and replaces their bodies with "throw new Error();".
 */
public class MethodPrunerVisitor extends ModifierVisitor<Void> {

  /**
   * The methods that should NOT be touched by this pruner. The strings representing the method are
   * those returned by ResolvedMethodDeclaration#getQualifiedSignature.
   */
  private Set<String> methodsToLeaveUnchanged;

  /**
   * The methods whose bodies should be pruned. The strings representing the method are those
   * returned by ResolvedMethodDeclaration#getQualifiedSignature.
   */
  private Set<String> methodsToEmpty;

  /**
   * This is the set of classes used by the target methods. We use this set to determine if we
   * should keep or delete an import statement. The strings representing the classes are in
   * the @FullyQualifiedName form.
   */
  private Set<String> classesUsedByTargetMethods;

  /**
   * This boolean tracks whether the element currently being visited is inside a target method. It
   * is set by {@link #visit(MethodDeclaration, Void)}.
   */
  private boolean insideTargetMethod = false;

  /**
   * Creates the pruner. All methods this pruner encounters other than those in its input sets will
   * be removed entirely. For both arguments, the Strings should be in the format produced by
   * ResolvedMethodDeclaration#getQualifiedSignature.
   *
   * @param methodsToKeep the set of methods whose bodies should be kept intact (usually the target
   *     methods for specimin)
   * @param methodsToEmpty the set of methods whose bodies should be removed
   */
  public MethodPrunerVisitor(
      Set<String> methodsToKeep,
      Set<String> methodsToEmpty,
      Set<String> classesUsedByTargetMethods) {
    this.methodsToLeaveUnchanged = methodsToKeep;
    this.methodsToEmpty = methodsToEmpty;
    this.classesUsedByTargetMethods = classesUsedByTargetMethods;
  }

  @Override
  public Node visit(ImportDeclaration decl, Void p) {
    String classFullName = decl.getNameAsString();
    if (classesUsedByTargetMethods.contains(classFullName)) {
      return super.visit(decl, p);
    }
    decl.remove();
    return decl;
  }

  @Override
  public Visitable visit(MethodDeclaration methodDecl, Void p) {
    ResolvedMethodDeclaration resolved = methodDecl.resolve();
    if (methodsToLeaveUnchanged.contains(resolved.getQualifiedSignature())) {
      insideTargetMethod = true;
      Visitable result = super.visit(methodDecl, p);
      insideTargetMethod = false;
      return result;
    } else if (methodsToEmpty.contains(resolved.getQualifiedSignature())) {
      methodDecl.setBody(StaticJavaParser.parseBlock("{ throw new Error(); }"));
      return methodDecl;
    } else {
      // if insideTargetMethod is true, this current method declaration belongs to an anonnymous
      // class inside the target method.
      if (!insideTargetMethod) {
        methodDecl.remove();
      }
      return methodDecl;
    }
  }

  @Override
  public Visitable visit(ConstructorDeclaration constructorDecl, Void p) {
    ResolvedConstructorDeclaration resolved = constructorDecl.resolve();
    if (methodsToLeaveUnchanged.contains(resolved.getQualifiedSignature())) {
      return super.visit(constructorDecl, p);
    } else if (methodsToEmpty.contains(resolved.getQualifiedSignature())) {
      constructorDecl.setBody(StaticJavaParser.parseBlock("{ throw new Error(); }"));
      return constructorDecl;
    } else {
      constructorDecl.remove();
      return constructorDecl;
    }
  }
}
