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
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.parametrization.ResolvedTypeParametersMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * If a used class includes methods that must be implemented (because it extends an abstract class
 * or implements an interface that requires them), this visitor marks them for preservation. Should
 * run after the list of used classes is finalized.
 */
public class MustImplementMethodsVisitor extends SpeciminStateVisitor {
  /**
   * Constructs a new SolveMethodOverridingVisitor with the provided sets of target methods, used
   * members, and used classes.
   *
   * @param previousVisitor the last visitor to run
   */
  public MustImplementMethodsVisitor(SpeciminStateVisitor previousVisitor) {
    super(previousVisitor);
  }

  @Override
  @SuppressWarnings("nullness:return") // ok to return null, because this is a void visitor
  public Visitable visit(ClassOrInterfaceDeclaration type, Void p) {
    if (type.getFullyQualifiedName().isPresent()
        && usedTypeElements.contains(type.getFullyQualifiedName().get())) {
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
    // However, if the current method is in a target type and in an interface/abstract class,
    // we don't need to preserve the method since there are no children that require its definition
    if (isPreservedAndAbstract(overridden)
        || (overridden == null
            && overridesAnInterfaceMethod(method)
            && !isParentTargetAndInterfaceOrAbstract(method))) {
      ResolvedMethodDeclaration resolvedMethod = method.resolve();
      Map<String, ResolvedType> returnAndParamAndThrowTypes = new HashMap<>();
      try {
        returnAndParamAndThrowTypes.put(
            resolvedMethod.getReturnType().describe(), resolvedMethod.getReturnType());
        for (int i = 0; i < resolvedMethod.getNumberOfParams(); ++i) {
          ResolvedParameterDeclaration param = resolvedMethod.getParam(i);
          returnAndParamAndThrowTypes.put(param.describeType(), param.getType());
        }
        for (ReferenceType thrownException : method.getThrownExceptions()) {
          ResolvedType resolvedException = thrownException.resolve();
          returnAndParamAndThrowTypes.put(resolvedException.describe(), resolvedException);
        }
      } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
        // In this case, don't keep the method (it won't compile anyway,
        // since some needed symbol isn't available). TODO: find a way to trigger the
        // creation of a synthetic class for the unsolved symbol at this point.
        return super.visit(method, p);
      }
      usedMembers.add(resolvedMethod.getQualifiedSignature());
      for (String type : returnAndParamAndThrowTypes.keySet()) {
        String originalType = type;
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

        boolean previouslyIncluded = usedTypeElements.contains(type);

        usedTypeElements.add(type);

        ResolvedType resolvedType = returnAndParamAndThrowTypes.get(originalType);

        if (!previouslyIncluded && resolvedType != null && resolvedType.isReferenceType()) {
          addAllResolvableAncestors(resolvedType.asReferenceType());
        }
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
   * Returns true iff the parent is an abstract class/interface and if it is a targeted type.
   *
   * @param node the Node to check
   * @return true iff the parent is a target abstract class/interface
   */
  private boolean isParentTargetAndInterfaceOrAbstract(Node node) {
    Node parent = JavaParserUtil.getEnclosingClassLike(node);

    if (parent instanceof ClassOrInterfaceDeclaration
        && (((ClassOrInterfaceDeclaration) parent).isInterface()
            || ((ClassOrInterfaceDeclaration) parent).isAbstract())) {
      String enclosingClassName =
          ((ClassOrInterfaceDeclaration) parent).getFullyQualifiedName().orElse(null);

      if (enclosingClassName != null) {
        for (String targetMethod : targetMethods) {
          if (targetMethod.startsWith(enclosingClassName)) {
            return true;
          }
        }
        for (String targetField : targetFields) {
          if (targetField.startsWith(enclosingClassName)) {
            return true;
          }
        }
      }
    }
    return false;
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
      Set<ResolvedReferenceType> parents =
          convertToResolvedReferenceTypes(asEnum.getImplementedTypes());

      return overridesAnInterfaceMethodImpl(parents, signature);
    } else if (typeElt instanceof ClassOrInterfaceDeclaration) {
      ClassOrInterfaceDeclaration asClass = (ClassOrInterfaceDeclaration) typeElt;

      // Get directly implemented interfaces as well as types implemented through parent classes
      Set<ResolvedReferenceType> parents =
          convertToResolvedReferenceTypes(asClass.getImplementedTypes());
      parents.addAll(convertToResolvedReferenceTypes(asClass.getExtendedTypes()));

      return overridesAnInterfaceMethodImpl(parents, signature);
    } else {
      throw new RuntimeException(
          "unexpected enclosing structure " + typeElt + " for method " + method);
    }
  }

  /**
   * Adds all resolvable ancestors (interfaces, superclasses) to the usedTypeElements set. It is
   * intended to be used when the type is not already present in usedTypeElements, but there is no
   * harm in calling this elsewhere.
   *
   * @param resolvedType the reference type to add its ancestors
   */
  private void addAllResolvableAncestors(ResolvedReferenceType resolvedType) {
    // If this method is called, this type is not used anywhere else except in this location
    // Therefore, its inherited types (if solvable) should be preserved since it was
    // not able to be preserved elsewhere.
    for (ResolvedReferenceType implementation :
        getAllImplementations(new HashSet<>(resolvedType.getAllAncestors()))) {
      usedTypeElements.add(implementation.getQualifiedName());
    }
  }

  /**
   * Helper method for overridesAnInterfaceMethod, to allow the same code to be shared in the enum
   * and class cases.
   *
   * @param implementedTypes the types of the implemented interfaces/classes
   * @param signature the signature we're looking for
   * @return see {@link #overridesAnInterfaceMethod(MethodDeclaration)}
   */
  private boolean overridesAnInterfaceMethodImpl(
      Set<ResolvedReferenceType> implementedTypes, String signature) {
    // Classes may exist in this collection; their primary purpose is to exclude a method
    // if a concrete method declaration exists
    Collection<ResolvedReferenceType> allImplementedTypes = getAllImplementations(implementedTypes);

    boolean result = false;

    for (ResolvedReferenceType resolvedInterface : allImplementedTypes) {
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
        // Escape localViewpointName in case it contains special regex characters like []
        // if the type is an array, for example
        localViewpointName = Pattern.quote(localViewpointName);
        targetSignature = targetSignature.replaceAll(localViewpointName, interfaceViewpointName);
      }
      // Type parameters in the types are erased (as they would be by javac when doing method
      // dispatching).
      // This means e.g. that a parameter with the type java.util.Collection<?> will become
      // java.util.Collection
      // (i.e., a raw type). Note though that this doesn't mean there are no type variables in the
      // signature:
      // add(E) is still add(E).
      targetSignature = JavaParserUtil.erase(targetSignature);

      for (ResolvedMethodDeclaration methodInInterface :
          resolvedInterface.getAllMethodsVisibleToInheritors()) {
        try {
          if (JavaParserUtil.erase(methodInInterface.getSignature()).equals(targetSignature)) {
            if (methodInInterface.isAbstract()) {
              // once we've found the correct method, we return to whether we
              // control it or not. If we don't, it must be preserved. If we do, then we only
              // preserve it if the PrunerVisitor won't remove it.
              if (!inOutput || usedMembers.contains(methodInInterface.getQualifiedSignature())) {
                // Do not immediately return; if two ancestors, unincluded interfaces are present,
                // one with a method declaration and one without, we need to return false even if
                // this may be true (depends on which method is traversed first)
                result = true;
                continue;
              }
            } else if (!inOutput) {
              // If we can't control the method, and there's a definition provided, we can safely
              // remove all overridden versions
              return false;
            }
          }
        } catch (UnsolvedSymbolException ex) {
          // since we are going through all ancestor interfaces/abstract classes, we should
          // expect that some method signature cannot be resolved. if this is the case, then
          // it's definitely not the method we're looking for.
          continue;
        }
      }
    }
    // if we don't find an overridden method in any of the implemented interfaces, return false.
    // however, if this method only implements abstract methods we can't control, then return true.
    return result;
  }

  /**
   * Helper method to convert ClassOrInterfaceTypes to ResolvedReferenceTypes
   *
   * @param types A List of interface/class types to convert
   * @return A set of ResolvedReferenceTypes representing the resolved input types
   */
  private static Set<ResolvedReferenceType> convertToResolvedReferenceTypes(
      List<ClassOrInterfaceType> types) {
    Set<ResolvedReferenceType> resolvedTypes = new HashSet<>();

    for (ClassOrInterfaceType type : types) {
      try {
        ResolvedReferenceType resolved =
            JavaParserUtil.classOrInterfaceTypeToResolvedReferenceType(type);
        resolvedTypes.add(resolved);
      } catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
        // In this case, we're implementing an interface that we don't control
        // or that will not be preserved.
        continue;
      }
    }

    return resolvedTypes;
  }

  /**
   * Gets all interface implementations of a List of ClassOrInterfaceTypes, including those of
   * ancestors. This method is intended to be used for interface / class implementations only (i.e.
   * pass in ClassOrInterfaceDeclaration.getImplementedTypes() or getExtendedTypes()).
   *
   * @param toTraverse A List of resolved reference types to find all ancestors
   * @return A Collection of ResolvedReferenceTypes containing all ancestors
   */
  private static Collection<ResolvedReferenceType> getAllImplementations(
      Set<ResolvedReferenceType> toTraverse) {
    Map<String, ResolvedReferenceType> qualifiedNameToType = new HashMap<>();
    while (!toTraverse.isEmpty()) {
      Set<ResolvedReferenceType> newToTraverse = new HashSet<>();
      for (ResolvedReferenceType type : toTraverse) {
        if (!qualifiedNameToType.containsKey(type.getQualifiedName())) {
          qualifiedNameToType.put(type.getQualifiedName(), type);
          for (ResolvedReferenceType implemented : type.getAllAncestors()) {
            newToTraverse.add(implemented);
          }
        }
      }
      toTraverse.clear();
      toTraverse = newToTraverse;
    }

    return qualifiedNameToType.values();
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
