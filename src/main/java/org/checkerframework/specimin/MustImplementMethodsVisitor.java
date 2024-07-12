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
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
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

  /** for checking if class files are in the original codebase. */
  private Map<String, Path> existingClassesToFilePath;

  /**
   * Constructs a new SolveMethodOverridingVisitor with the provided sets of target methods, used
   * members, and used classes.
   *
   * @param usedMembers Set containing the signatures of used members.
   * @param usedClass Set containing the signatures of used classes.
   * @param existingClassesToFilePath map from existing classes to file paths
   */
  public MustImplementMethodsVisitor(
      Set<String> usedMembers, Set<String> usedClass, Map<String, Path> existingClassesToFilePath) {
    this.usedMembers = usedMembers;
    this.usedClass = usedClass;
    this.existingClassesToFilePath = existingClassesToFilePath;
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
    ResolvedMethodDeclaration overridden = getOverriddenMethodInSuperClass(method);
    // two cases: the method is a solvable override that will be preserved, and we can show that
    // it is abstract (before the ||), or we can't solve the override but there
    // is an @Override annotation. This relies on the use of @Override when
    // implementing required methods from interfaces in the target code,
    // but unfortunately I think it's the best that we can do here. (@Override
    // is technically optional, but it is widely used.)
    if (isPreservedAndAbstract(overridden)
        || (overridden == null && overridesAnInterfaceMethod(method))) {
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
    if (method == null || !method.isAbstract()) {
      return false;
    }
    String methodSignature = method.getQualifiedSignature();
    // These classes are beyond our control. It's better to retain the implementations of all
    // abstract methods to ensure the code remains compilable.
    if (JavaLangUtils.inJdkPackage(methodSignature)) {
      return true;
    }
    return usedMembers.contains(methodSignature);
  }

  /**
   * Returns true iff the given method declaration is overriding a preserved unimplemented method in
   * an implemented interface. This is an expensive check that searches the implemented interfaces.
   *
   * @param method the method declaration to check
   * @return true iff the given method definitely overrides a preserved method in an interface
   */
  private boolean overridesAnInterfaceMethod(MethodDeclaration method) {
    ResolvedMethodDeclaration resolved;
    String signature;
    try {
      resolved = method.resolve();
      signature = resolved.getSignature();
    } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
      // Some part of the signature isn't being preserved, so this shouldn't be preserved,
      // either.
      return false;
    }
    Node typeElt = JavaParserUtil.getEnclosingClassLike(method);

    if (typeElt instanceof EnumDeclaration) {
      EnumDeclaration asEnum = (EnumDeclaration) typeElt;
      return overridesAnInterfaceMethodImpl(asEnum.getImplementedTypes(), signature);
    } else if (typeElt instanceof ClassOrInterfaceDeclaration) {
      ClassOrInterfaceDeclaration asClass = (ClassOrInterfaceDeclaration) typeElt;
      return overridesAnInterfaceMethodImpl(asClass.getImplementedTypes(), signature);
    } else {
      throw new RuntimeException(
          "unexpected enclosing structure " + typeElt + " for method " + method);
    }
  }

  /**
   * Helper method for overridesAnInterfaceMethod, to allow the same code to be shared in the enum
   * and class cases.
   *
   * @param implementedTypes the types of the implemented interfaces
   * @param signature the signature we're looking for
   * @return see {@link #overridesAnInterfaceMethod(MethodDeclaration)}
   */
  private boolean overridesAnInterfaceMethodImpl(
      NodeList<ClassOrInterfaceType> implementedTypes, String signature) {
    for (ClassOrInterfaceType implementedType : implementedTypes) {
      ResolvedReferenceType resolvedInterface;
      try {
        resolvedInterface =
            JavaParserUtil.classOrInterfaceTypeToResolvedReferenceType(implementedType);
      } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
        // In this case, we're implementing an interface that we don't control
        // or that will not be preserved.
        continue;
      }
      // This boolean is important to distinguish between the case of
      // an interface that's in the input/output (and therefore could change)
      // and an interface that's not, such as java.util.Set from the JDK. For
      // the latter, we need to preserve required overrides in all cases, even if
      // they are not used. For the former, we only need to preserve required overrides
      // if the method is actually invoked (if not, it will be removed from the interface
      // elsewhere).
      boolean inOutput =
          this.existingClassesToFilePath.containsKey(resolvedInterface.getQualifiedName());

      // It's necessary to viewpoint-adapt the type parameters so that the signature we're looking
      // for matches the one that we'll find in the interface's definition. This code
      // substitutes type variables in reverse: the target signature is adjusted to match
      // the view of the type parameters from the perspective of the interface. For example,
      // if the implemented interface is Set<V>, this code will change the target signature
      // add(V) to add(E), because in the definition of java.util.Set the type variable is
      // called E.
      ResolvedTypeParametersMap typeParametersMap = resolvedInterface.typeParametersMap();
      String targetSignature = signature;
      for (String name : typeParametersMap.getNames()) {
        String interfaceViewpointName = name.substring(name.lastIndexOf('.') + 1);
        String localViewpointName = typeParametersMap.getValueBySignature(name).get().describe();
        targetSignature = targetSignature.replaceAll(localViewpointName, interfaceViewpointName);
      }
      // Type parameters in the types are erased (as they would be by javac when doing method
      // dispatching).
      // This means e.g. that a parameter with the type java.util.Collection<?> will become
      // java.util.Collection
      // (i.e., a raw type). Note though that this doesn't mean there are no type variables in the
      // signature:
      // add(E) is still add(E).
      targetSignature = erase(targetSignature);

      for (ResolvedMethodDeclaration methodInInterface :
          resolvedInterface.getAllMethodsVisibleToInheritors()) {
        if (methodInInterface.isAbstract()
            && erase(methodInInterface.getSignature()).equals(targetSignature)) {
          // once we've found the correct method, we return to whether we
          // control it or not. If we don't, it must be preserved. If we do, then we only
          // preserve it if the PrunerVisitor won't remove it.
          return !inOutput || usedMembers.contains(methodInInterface.getQualifiedSignature());
        }
      }
    }
    // if we don't find an overridden method in any of the implemented interfaces, return false.
    return false;
  }

  /**
   * Erases type arguments from a method signature string.
   *
   * @param signature the signature
   * @return the same signature without type arguments
   */
  private static String erase(String signature) {
    return signature.replaceAll("<.*>", "");
  }

  /**
   * Given a MethodDeclaration, this method returns the method that it overrides, if one exists in
   * one of its super classes. If one does not exist, it returns null.
   *
   * @param methodDeclaration the method declaration to check
   * @return the method that this method overrides, if one exists in a superclass. Null if no such
   *     method exists.
   */
  public static @Nullable ResolvedMethodDeclaration getOverriddenMethodInSuperClass(
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
