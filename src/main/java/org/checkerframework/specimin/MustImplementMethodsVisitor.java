package org.checkerframework.specimin;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.parametrization.ResolvedTypeParametersMap;
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
   * for checking if class files are in the original codebase. TODO: refactor so this information is
   * separate
   */
  private UnsolvedSymbolVisitor unsolvedSymbolVisitor;

  /**
   * Constructs a new SolveMethodOverridingVisitor with the provided sets of target methods, used
   * members, and used classes.
   *
   * @param usedMembers Set containing the signatures of used members.
   * @param usedClass Set containing the signatures of used classes.
   */
  public MustImplementMethodsVisitor(
      Set<String> usedMembers, Set<String> usedClass, UnsolvedSymbolVisitor unsolvedSymbolVisitor) {
    this.usedMembers = usedMembers;
    this.usedClass = usedClass;
    this.unsolvedSymbolVisitor = unsolvedSymbolVisitor;
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
    // two cases: the method is a solvable override that will be preserved, and we can show that
    // it is abstract (before the ||), or we can't solve the override but there
    // is an @Override annotation. This relies on the use of @Override when
    // implementing required methods from interfaces in the target code,
    // but unfortunately I think it's the best that we can do here. (@Override
    // is technically optional, but it is widely used.)
    if (isPreservedAndAbstract(overridden) || (overridden == null && isOverride(method))) {
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
  private boolean isPreservedAndAbstract(@Nullable ResolvedMethodDeclaration method) {
    return method != null
        && method.isAbstract()
        && usedMembers.contains(method.getQualifiedSignature());
  }

  /**
   * Returns true iff the given method declaration is overriding a preserved unimplemented method in
   * an implemented interface. This is an expensive check that searches the implemented interfaces.
   *
   * @param method the method declaration to check
   * @return true iff the given method definitely overrides a preserved method
   */
  private boolean isOverride(MethodDeclaration method) {
    ResolvedMethodDeclaration resolved = method.resolve();
    String signature = resolved.getSignature();
    Node typeElt = PrunerVisitor.getEnclosingClassLike(method);

    // Whether or not to fall back on the presence of an @Override annotation. We want
    // to avoid that as much as we can, but when we can't solve an implemented interface,
    // we'll be required to do so to get cases like AbstractImplTest correct (e.g., that
    // test contains a class that implement java.util.Set).
    boolean useFallback = false;

    if (typeElt instanceof EnumDeclaration) {
      EnumDeclaration enumDecl = (EnumDeclaration) typeElt;
      for (ClassOrInterfaceType implementedType : enumDecl.getImplementedTypes()) {
        try {
          implementedType.resolve();
        } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
          // in this case, we're implmenting an interface that we don't control. Fallback on
          // whether there is an @Override annotation.
          useFallback = true;
          continue;
        }
        for (Node member : implementedType.getChildNodes()) {
          if (member instanceof MethodDeclaration) {
            MethodDeclaration memberAsDecl = (MethodDeclaration) member;
            try {
              ResolvedMethodDeclaration resolvedMemberDecl = memberAsDecl.resolve();
              if (resolvedMemberDecl.isAbstract()
                  && resolvedMemberDecl.getSignature().equals(signature)) {
                return true;
              }
            } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
              // expected to occur some of the time
            }
          }
        }
      }
    } else if (typeElt instanceof ClassOrInterfaceDeclaration) {
      ClassOrInterfaceDeclaration asClass = (ClassOrInterfaceDeclaration) typeElt;
      for (ClassOrInterfaceType implementedType : asClass.getImplementedTypes()) {
        ResolvedReferenceType resolvedInterface;
        try {
          resolvedInterface = implementedType.resolve();
        } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
          // in this case, we're implmenting an interface that we don't control. Fallback on
          // whether there is an @Override annotation.
          useFallback = true;
          continue;
        }
        boolean inOutput =
            unsolvedSymbolVisitor.classfileIsInOriginalCodebase(
                resolvedInterface.getQualifiedName());
        ResolvedTypeParametersMap typeParametersMap = resolvedInterface.typeParametersMap();
        String targetSignature = signature;
        for (String name : typeParametersMap.getNames()) {
          String simpleName = name.substring(name.lastIndexOf('.') + 1);
          String localName = typeParametersMap.getValueBySignature(name).get().describe();
          targetSignature = targetSignature.replaceAll(localName, simpleName);
        }
        targetSignature = erase(targetSignature);
        for (ResolvedMethodDeclaration methodInInterface :
            resolvedInterface.getAllMethodsVisibleToInheritors()) {
          if (methodInInterface.isAbstract()
              && erase(methodInInterface.getSignature()).equals(targetSignature)) {
            boolean result =
                !inOutput || usedMembers.contains(methodInInterface.getQualifiedSignature());
            return result;
          }
        }
      }
    } else {
      throw new RuntimeException(
          "unexpected enclosing structure " + typeElt + " for method " + method);
    }
    // if useFallback is false, this always returns false. Otherwise, the presence/absence
    // of an @Override annotation is the deciding factor.
    return useFallback
        && (method.getAnnotationByName("Override").isPresent()
            || method.getAnnotationByName("java.lang.Override").isPresent());
  }

  private static String erase(String signature) {
    return signature.replaceAll("<.*>", "");
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
