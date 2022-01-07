package org.checkerframework.specimin;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import java.util.HashSet;
import java.util.Set;

/**
 * The main visitor for Specimin's' first phase, which locates the target method(s) and compiles
 * information on what specifications they use.
 */
public class TargetMethodFinderVisitor extends ModifierVisitor<Void> {
  /**
   * The names of the target methods. The format is
   * class.fully.qualified.Name#methodName(Param1Type, Param2Type, ...)
   */
  private Set<String> targetMethodNames;

  /**
   * This boolean tracks whether the element currently being visited is inside a target method. It
   * is set by {@link #visit(MethodDeclaration, Void)}.
   */
  private boolean insideTargetMethod = false;

  /** The fully-qualified name of the class currently being visited. */
  private String classFQName = "";

  /**
   * The methods that were actually used by the targets, and therefore ought to have their
   * specifications (but not bodies) preserved.
   */
  private Set<ResolvedMethodDeclaration> usedMethods = new HashSet<>();

  /** The resolved target methods. */
  private Set<ResolvedMethodDeclaration> targetMethods = new HashSet<>();

  /**
   * Create a new target method finding visitor.
   *
   * @param methodNames the names of the target methods, the format
   *     class.fully.qualified.Name#methodName(Param1Type, Param2Type, ...)
   */
  public TargetMethodFinderVisitor(String... methodNames) {
    targetMethodNames = new HashSet<>();
    for (String methodName : methodNames) {
      targetMethodNames.add(methodName);
    }
  }

  /**
   * Get the methods that this visitor has concluded that the target method(s) use, and therefore
   * ought to be retained.
   *
   * @return the used methods
   */
  public Set<ResolvedMethodDeclaration> getUsedMethods() {
    return usedMethods;
  }

  /**
   * Get the target methods that this visitor has encountered so far.
   *
   * @return the target methods
   */
  public Set<ResolvedMethodDeclaration> getTargetMethods() {
    return targetMethods;
  }

  @Override
  public Visitable visit(ClassOrInterfaceDeclaration decl, Void p) {
    if (decl.isInnerClass()) {
      this.classFQName += "." + decl.getName().toString();
    } else {
      if (!this.classFQName.equals("")) {
        throw new UnsupportedOperationException(
            "Attempted to enter an unexpected kind of class: "
                + decl.getFullyQualifiedName()
                + " but already had a set classFQName: "
                + classFQName);
      }
      // Should always be present.
      this.classFQName = decl.getFullyQualifiedName().orElseThrow();
    }
    Visitable result = super.visit(decl, p);
    if (decl.isInnerClass()) {
      this.classFQName = this.classFQName.substring(0, this.classFQName.lastIndexOf('.'));
    } else {
      this.classFQName = "";
    }
    return result;
  }

  @Override
  public Visitable visit(MethodDeclaration method, Void p) {
    // TODO: check whether this is a target method where the "true" is on the next line.
    String methodDeclAsString = method.getDeclarationAsString(false, false, false);
    // The substring here is to remove the method's return type. Return types cannot contain spaces.
    // TODO: test this with annotations
    String methodName =
        this.classFQName + "#" + methodDeclAsString.substring(methodDeclAsString.indexOf(' ') + 1);
    System.out.println(methodName);
    if (this.targetMethodNames.contains(methodName)) {
      insideTargetMethod = true;
      targetMethods.add(method.resolve());
    }
    Visitable result = super.visit(method, p);
    insideTargetMethod = false;
    return result;
  }

  @Override
  public Visitable visit(MethodCallExpr call, Void p) {
    if (insideTargetMethod) {
      usedMethods.add(call.resolve());
    }
    return super.visit(call, p);
  }
}
