package org.checkerframework.specimin;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * If a used class includes methods that must be implemented (because it extends an abstract class
 * or implements an interface that requires them), this visitor marks them for preservation. Should
 * run after the list of used classes is finalized.
 */
public class MustImplementMethodsVisitor extends ModifierVisitor<Void> {

  /** Set containing the signatures of used member (fields and methods). */
  private Set<String> usedMembers;

  /** Set containing the signatures of used classes. */
  private Set<String> usedClass;

  /**
   * Constructs a new SolveMethodOverridingVisitor with the provided sets of target methods, used
   * members, and used classes.
   *
   * @param usedMembers Set containing the signatures of used members.
   * @param usedClass Set containing the signatures of used classes.
   */
  public MustImplementMethodsVisitor(Set<String> usedMembers, Set<String> usedClass) {
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
  @SuppressWarnings("nullness:return") // ok to return null, because this is a void visitor
  public Visitable visit(ClassOrInterfaceDeclaration type, Void p) {
    if (type.getFullyQualifiedName().isPresent()
        && usedClass.contains(type.getFullyQualifiedName().get())) {
      return super.visit(type, p);
    } else {
      // the effect of not calling super here is that only used classes
      // will actually be visited by this class
      return null;
    }
  }

  @Override
  public Visitable visit(MethodDeclaration method, Void p) {
    ResolvedMethodDeclaration overridden = getOverriddenMethod(method);
    // two cases: the method is a solvable override, and we can show that
    // it is abstract (before the ||), or we can't solve the override but there
    // is an @Override annotation. This relies on the use of @Override when
    // implementing required methods from interfaces in the target code,
    // but unfortunately I think it's the best that we can do here. (@Override
    // is technically optional, but it is widely used.)
    if (isAbstract(overridden) || (overridden == null && isOverride(method))) {
      ResolvedMethodDeclaration resolvedMethod = method.resolve();
      Set<String> returnAndParamTypes = new HashSet<>();
      try {
        returnAndParamTypes.add(resolvedMethod.getReturnType().describe());
        for (int i = 0; i < resolvedMethod.getNumberOfParams(); ++i) {
          ResolvedParameterDeclaration param = resolvedMethod.getParam(i);
          returnAndParamTypes.add(param.describeType());
        }
      } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
        // In this case, don't keep the method (it won't compile anyway,
        // since some needed symbol isn't available). TODO: find a way to trigger the
        // creation of a synthetic class for the unsolved symbol at this point.
        return super.visit(method, p);
      }
      usedMembers.add(resolvedMethod.getQualifiedSignature());
      for (String type : returnAndParamTypes) {
        type = type.trim();
        if (type.contains("<")) {
          // remove generics, if present, since this type will be used in
          // an import
          type = type.substring(0, type.indexOf("<"));
        }
        // also remove array types
        if (type.contains("[]")) {
          type = type.replace("[]", "");
        }
        usedClass.add(type);
      }
    }
    return super.visit(method, p);
  }

  /**
   * Returns true if the given method is abstract.
   *
   * @param method a possibly-null method declaration
   * @return true iff the input is non-null and abstract
   */
  private boolean isAbstract(@Nullable ResolvedMethodDeclaration method) {
    return method != null && method.isAbstract();
  }

  /**
   * Returns true iff the given method declaration has an @Override annotation. This is a coarse
   * approximation (@Override is optional, unfortunately), but it's the best we can do given the
   * limits of JavaParser.
   *
   * @param method the method declaration to check
   * @return true iff there is an override annotation on the given method
   */
  private boolean isOverride(MethodDeclaration method) {
    return method.getAnnotationByName("Override").isPresent()
        || method.getAnnotationByName("java.lang.Override").isPresent();
  }

  /**
   * Given a MethodDeclaration, this method returns the method that it overrides, if one exists. If
   * not, it returns null.
   */
  public static @Nullable ResolvedMethodDeclaration getOverriddenMethod(
      MethodDeclaration methodDeclaration) {
    // just a method signature, no need to check for overriding.
    if (methodDeclaration.getBody().isEmpty()) {
      return null;
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
    ResolvedMethodDeclaration resolvedSuperCall = null;
    try {
      resolvedSuperCall = superCall.resolve();
    } catch (Exception e) {
      // The current method is not overriding, thus the super call is unresolved.
      // This catch block is necessary to avoid crashes due to ignored catch blocks. A single
      // remove() call is not enough to remove a MethodCallExpr.
      superCall.remove();
    }
    JavaParserUtil.removeNode(superCall);
    return resolvedSuperCall;
  }
}
