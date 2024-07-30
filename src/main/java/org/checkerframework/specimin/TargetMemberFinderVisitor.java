package org.checkerframework.specimin;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedWildcard;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The main visitor for Specimin's first phase, which locates the target member(s) and compiles
 * information on what specifications they use.
 */
public class TargetMemberFinderVisitor extends SpeciminStateVisitor {
  /**
   * The names of the target methods. The format is
   * class.fully.qualified.Name#methodName(Param1Type, Param2Type, ...). All the names will have
   * spaces remove for ease of comparison.
   */
  private final Set<String> targetMethodNames;

  /** The name of the package currently being visited. */
  private String currentPackage = "";

  /**
   * The keys of this map are a local copy of the input list of methods. A method is removed from
   * this copy's key set when it is located. If the visitor has been run on all source files and the
   * key set isn't empty, that usually indicates an error. The values are other method signatures
   * that were found in the key's class, but which were not the key. These values are only used for
   * error reporting: it is useful to the user if they make a typo to know what the correct
   * name/signature of a method actually is.
   */
  private final Map<String, Set<String>> unfoundMethods;

  /** Same as the unfoundMethods set, but for fields */
  private final Map<String, Set<String>> unfoundFields = new HashMap<>();

  /**
   * This map connects the resolved declaration of a method to the interface that contains it, if
   * any.
   */
  private final Map<ResolvedMethodDeclaration, ClassOrInterfaceType>
      methodDeclarationToInterfaceType = new HashMap<>();

  /**
   * This map connects the fully-qualified names of non-primary classes with the fully-qualified
   * names of their corresponding primary classes. A primary class is a class that has the same name
   * as the Java file where the class is declared.
   */
  Map<String, String> nonPrimaryClassesToPrimaryClass;

  /**
   * JavaParser is not perfect. Sometimes it can't solve resolved method calls if they have
   * complicated type variables or if the receiver is the parameter of a lambda expression. We keep
   * track of these stuck method calls and preserve them anyway. There are two possible formats for
   * the strings in this set: fully-qualified method names (which will be directly preserved) and
   * unqualified method names with a {@literal @} symbol and the number of parameters that they take
   * appended. Anything that matches the latter will later be preserved.
   */
  private final Set<String> resolvedYetStuckMethodCall = new HashSet<>();

  /**
   * Create a new target method finding visitor.
   *
   * @param previous the previous Specimin visitor
   * @param nonPrimaryClassesToPrimaryClass map connecting non-primary classes with their
   *     corresponding primary classes
   */
  public TargetMemberFinderVisitor(
      SpeciminStateVisitor previous, Map<String, String> nonPrimaryClassesToPrimaryClass) {
    super(previous);
    targetMethodNames = new HashSet<>();
    for (String methodSignature : targetMethods) {
      this.targetMethodNames.add(methodSignature.replaceAll("\\s", ""));
    }
    unfoundMethods = new HashMap<>(targetMethods.size());
    targetMethodNames.forEach(m -> unfoundMethods.put(m, new HashSet<>()));
    targetFields.forEach(f -> unfoundFields.put(f, new HashSet<>()));
    this.nonPrimaryClassesToPrimaryClass = nonPrimaryClassesToPrimaryClass;
  }

  /**
   * Returns the methods that so far this visitor has not located from its target list. Usually,
   * this should be checked after running the visitor to ensure that it is empty. The targets are
   * the keys in the returned maps; the values are methods in the same class that were considered
   * but were not the target (useful for issuing error messages).
   *
   * @return the methods that so far this visitor has not located from its target list, mapped to
   *     the candidate methods that were considered
   */
  public Map<String, Set<String>> getUnfoundMethods() {
    return unfoundMethods;
  }

  /**
   * Returns the fields that so far this visitor has not located from its target list. Usually, this
   * should be checked after running the visitor to ensure that it is empty. The targets are the
   * keys in the returned maps; the values are fields in the same class that were considered but
   * were not the target (useful for issuing error messages).
   *
   * @return the fields that so far this visitor has not located from its target list, mapped to the
   *     candidate fields that were considered
   */
  public Map<String, Set<String>> getUnfoundFields() {
    return unfoundFields;
  }

  /**
   * Get the set of resolved yet stuck method calls.
   *
   * @return the value of stuck methods.
   */
  public Set<String> getResolvedYetStuckMethodCall() {
    return resolvedYetStuckMethodCall;
  }

  /**
   * Updates the mapping of method declarations to their corresponding interface type based on a
   * list of methods and the interface type that contains those methods.
   *
   * @param methodList the list of resolved method declarations
   * @param interfaceType the interface containing the specified methods.
   */
  private void updateMethodDeclarationToInterfaceType(
      List<ResolvedMethodDeclaration> methodList, ClassOrInterfaceType interfaceType) {
    for (ResolvedMethodDeclaration method : methodList) {
      this.methodDeclarationToInterfaceType.put(method, interfaceType);
    }
  }

  /**
   * Updates unfoundMethods so that the appropriate elements have their set of considered methods
   * updated to match a method that was not a target method.
   *
   * @param methodAsString the method that wasn't a target method
   */
  private void updateUnfoundMethods(String methodAsString) {
    Set<String> targetMethodsInClass =
        targetMethodNames.stream()
            .filter(t -> t.startsWith(this.currentClassQualifiedName))
            .collect(Collectors.toSet());

    for (String targetMethodInClass : targetMethodsInClass) {
      // This check is necessary to avoid an NPE if the target method
      // in question has already been removed from unfoundMethods.
      if (unfoundMethods.containsKey(targetMethodInClass)) {
        unfoundMethods.get(targetMethodInClass).add(methodAsString);
      }
    }
  }

  /**
   * Updates unfoundFields so that the appropriate elements have their set of considered fields
   * updated to match a field that was not a target field.
   *
   * @param fieldAsString the field that wasn't a target field
   */
  private void updateUnfoundFields(String fieldAsString) {
    Set<String> targetFieldsInClass =
        targetFields.stream()
            .filter(t -> t.startsWith(this.currentClassQualifiedName))
            .collect(Collectors.toSet());

    for (String targetFieldInClass : targetFieldsInClass) {
      // This check is necessary to avoid an NPE if the target field
      // in question has already been removed from unfoundFields.
      if (unfoundFields.containsKey(targetFieldInClass)) {
        unfoundFields.get(targetFieldInClass).add(fieldAsString);
      }
    }
  }

  @Override
  public Visitable visit(PackageDeclaration decl, Void p) {
    this.currentPackage = decl.getNameAsString();
    return super.visit(decl, p);
  }

  @Override
  public Visitable visit(ClassOrInterfaceDeclaration decl, Void p) {
    for (ClassOrInterfaceType interfaceType : decl.getImplementedTypes()) {
      try {
        updateMethodDeclarationToInterfaceType(
            JavaParserUtil.classOrInterfaceTypeToResolvedReferenceType(interfaceType)
                .getAllMethods(),
            interfaceType);
      } catch (UnsolvedSymbolException e) {
        continue;
      }
    }

    return super.visit(decl, p);
  }

  @Override
  public Visitable visit(ConstructorDeclaration method, Void p) {
    String constructorMethodAsString = method.getDeclarationAsString(false, false, false);
    // the methodName will be something like this: "com.example.Car#Car()"
    String methodName = this.currentClassQualifiedName + "#" + constructorMethodAsString;
    // remove spaces
    methodName = methodName.replaceAll("\\s", "");
    if (this.targetMethodNames.contains(methodName)) {
      ResolvedConstructorDeclaration resolvedMethod = method.resolve();
      targetMethods.add(resolvedMethod.getQualifiedSignature());
      unfoundMethods.remove(methodName);
      updateUsedClassWithQualifiedClassName(
          resolvedMethod.getPackageName() + "." + resolvedMethod.getClassName(),
          usedTypeElements,
          nonPrimaryClassesToPrimaryClass);
      if (modularityModel.preserveAllFieldsIfTargetIsConstructor()) {
        // This cast is safe, because a constructor must be contained in a class declaration.
        ClassOrInterfaceDeclaration thisClass =
            (ClassOrInterfaceDeclaration) JavaParserUtil.getEnclosingClassLike(method);
        for (FieldDeclaration field : thisClass.getFields()) {
          for (VariableDeclarator variable : field.getVariables()) {
            usedMembers.add(currentClassQualifiedName + "#" + variable.getNameAsString());
            ResolvedType fieldType = variable.resolve().getType();
            updateUsedClassBasedOnType(fieldType);
          }
        }
      }
    } else {
      updateUnfoundMethods(methodName);
    }

    Visitable result = super.visit(method, p);

    if (method.getParentNode().isEmpty()) {
      return result;
    }
    if (method.getParentNode().get() instanceof EnumDeclaration) {
      EnumDeclaration parentNode = (EnumDeclaration) method.getParentNode().get();
      if (parentNode.getFullyQualifiedName().isEmpty()) {
        return result;
      }
      // used enums needs to have compilable constructors.
      if (usedTypeElements.contains(parentNode.getFullyQualifiedName().orElseThrow())) {
        for (Parameter parameter : method.getParameters()) {
          updateUsedClassBasedOnType(parameter.getType().resolve());
        }
      }
    }
    return result;
  }

  @Override
  public Visitable visit(VariableDeclarator node, Void arg) {
    if (node.getParentNode().isPresent()
        && node.getParentNode().get() instanceof FieldDeclaration) {
      String fieldName = this.currentClassQualifiedName + "#" + node.getNameAsString();
      if (targetFields.contains(fieldName)) {
        ResolvedFieldDeclaration resolvedField =
            ((FieldDeclaration) node.getParentNode().get()).resolve();
        unfoundFields.remove(fieldName);
        updateUsedClassWithQualifiedClassName(
            resolvedField.declaringType().getQualifiedName(),
            usedTypeElements,
            nonPrimaryClassesToPrimaryClass);
      } else {
        updateUnfoundFields(fieldName);
      }
    }
    return super.visit(node, arg);
  }

  @Override
  public Visitable visit(AssignExpr node, Void p) {
    if (insideTargetCtor) {
      // check if the LHS is a field
      Expression lhs = node.getTarget();
      if (lhs.isFieldAccessExpr()) {
        FieldAccessExpr asFieldAccess = lhs.asFieldAccessExpr();
        Expression scope = asFieldAccess.getScope();
        if (scope.toString().equals("this")) {
          fieldsAssignedByTargetCtors.add(
              currentClassQualifiedName + "#" + asFieldAccess.getNameAsString());
        }
      } else if (lhs.isNameExpr()) {
        // could be a field of "this"
        NameExpr asName = lhs.asNameExpr();
        ResolvedValueDeclaration resolved = asName.resolve();
        if (resolved.isField()) {
          fieldsAssignedByTargetCtors.add(
              currentClassQualifiedName + "#" + asName.getNameAsString());
        }
      }
    }
    return super.visit(node, p);
  }

  @Override
  public Visitable visit(MethodDeclaration method, Void p) {
    boolean oldInsideTargetMember = insideTargetMember;
    // TODO: test this with annotations
    String methodWithoutReturnAndAnnos =
        JavaParserUtil.removeMethodReturnTypeAndAnnotations(method);
    String methodName = this.currentClassQualifiedName + "#" + methodWithoutReturnAndAnnos;
    // this method belongs to an anonymous class inside the target method
    if (insideTargetMember) {
      Node parentNode = method.getParentNode().get();
      // it could also be an enum declaration, but those are handled separately
      if (parentNode instanceof ObjectCreationExpr) {
        ObjectCreationExpr parentExpression = (ObjectCreationExpr) parentNode;
        ResolvedConstructorDeclaration resolved = parentExpression.resolve();
        String methodPackage = resolved.getPackageName();
        String methodClass = resolved.getClassName();
        usedMembers.add(methodPackage + "." + methodClass + "." + method.getNameAsString() + "()");
        updateUsedClassWithQualifiedClassName(
            methodPackage + "." + methodClass, usedTypeElements, nonPrimaryClassesToPrimaryClass);
      }
    }
    String methodWithoutAnySpace = methodName.replaceAll("\\s", "");
    if (this.targetMethodNames.contains(methodWithoutAnySpace)) {
      ResolvedMethodDeclaration resolvedMethod = method.resolve();
      updateUsedClassesForInterface(resolvedMethod);
      updateUsedClassWithQualifiedClassName(
          resolvedMethod.getPackageName() + "." + resolvedMethod.getClassName(),
          usedTypeElements,
          nonPrimaryClassesToPrimaryClass);

      insideTargetMember = true;
      targetMethods.add(resolvedMethod.getQualifiedSignature());
      // make sure that differences in spacing does not interfere with the result
      for (String unfound : unfoundMethods.keySet()) {
        if (unfound.replaceAll("\\s", "").equals(methodWithoutAnySpace)) {
          unfoundMethods.remove(unfound);
          break;
        }
      }
      Type returnType = method.getType();
      // JavaParser may misinterpret unresolved array types as reference types.
      // To ensure accuracy, we resolve the type before proceeding with the check.
      try {
        ResolvedType resolvedType = returnType.resolve();
        if (resolvedType instanceof ResolvedReferenceType) {
          updateUsedClassBasedOnType(resolvedType);
        }
      } catch (UnsupportedOperationException e) {
        // Occurs if the type is a type variable, so there is nothing to do:
        // the type variable must have been declared in one of the containing scopes,
        // and UnsolvedSymbolVisitor should already guarantee that the variable will
        // be included in one of the classes that Specimin outputs.
      } catch (UnsolvedSymbolException e) {
        throw new RuntimeException(
            "failed to solve the return type ("
                + returnType
                + ") of "
                + methodWithoutReturnAndAnnos,
            e);
      }
    } else {
      updateUnfoundMethods(methodName);
    }

    Visitable result = super.visit(method, p);
    insideTargetMember = oldInsideTargetMember;
    return result;
  }

  @Override
  public Visitable visit(Parameter para, Void p) {
    if (insideTargetMember) {
      Type type = para.getType();
      // an unknown type plays the role of a null object for lambda parameters that have no explicit
      // type declared. However, we also want to avoid trying to solve declared lambda params (it
      // will
      // fail, despite the fact that the code should be compilable).
      boolean isLambdaParam = type.isUnknownType() || isLambdaParam(para);
      if (type.isUnionType()) {
        resolveUnionType(type.asUnionType());
      } else if (!isLambdaParam) {
        // Parameter resolution (para.resolve()) does not work in catch clause.
        // However, resolution works on the type of the parameter.
        // Bug report: https://github.com/javaparser/javaparser/issues/4240
        ResolvedType paramType;
        if (para.getParentNode().isPresent() && para.getParentNode().get() instanceof CatchClause) {
          paramType = para.getType().resolve();
        } else {
          try {
            paramType = para.resolve().getType();
          } catch (UnsupportedOperationException e) {
            throw new RuntimeException("cannot solve: " + para, e);
          }
        }

        if (paramType.isReferenceType()) {
          String paraTypeFullName =
              paramType.asReferenceType().getTypeDeclaration().orElseThrow().getQualifiedName();
          updateUsedClassWithQualifiedClassName(
              paraTypeFullName, usedTypeElements, nonPrimaryClassesToPrimaryClass);
          for (ResolvedType typeParameterValue :
              paramType.asReferenceType().typeParametersValues()) {
            String typeParameterValueName = typeParameterValue.describe();
            if (typeParameterValueName.contains("<")) {
              // removing the "<...>" part if there is any.
              typeParameterValueName =
                  typeParameterValueName.substring(0, typeParameterValueName.indexOf("<"));
            }
            updateUsedClassWithQualifiedClassName(
                typeParameterValueName, usedTypeElements, nonPrimaryClassesToPrimaryClass);
          }
        }
      }
    }

    return super.visit(para, p);
  }

  /**
   * Returns true iff we can prove that the parameter is a lambda parameter. This method should only
   * be called on parameters that are not of unknown type (which are definitely lambda params).
   *
   * @param para a parameter
   * @return true iff para is a (typed) parameter in a lambda
   */
  private boolean isLambdaParam(Parameter para) {
    return para.getParentNode().orElseThrow() instanceof LambdaExpr;
  }

  @Override
  public Visitable visit(MethodReferenceExpr ref, Void p) {
    if (insideTargetMember) {
      ResolvedMethodDeclaration decl = ref.resolve();
      preserveMethodDecl(decl);
    }
    return super.visit(ref, p);
  }

  @Override
  public Visitable visit(MethodCallExpr call, Void p) {
    if (insideTargetMember) {
      ResolvedMethodDeclaration decl;
      try {
        decl = call.resolve();
      } catch (UnsupportedOperationException e) {
        // This case only occurs when a method is called on a lambda parameter.
        // JavaParser has a type variable for the lambda parameter, but it won't
        // have any constraints (JavaParser isn't very good at solving lambda parameter
        // types). The approach here preserves any method that might be the callee that's
        // in the input (based on the simple name of the method and its number of parameters).
        // TODO: this approach is both unsound and imprecise but works most of the time on
        // real examples. A better approach would be to either:
        // * update to a new version of JavaParser that _can_ solve lambda parameters
        // (we believe that newer JP versions are much improved), or
        // * add another javac pass after pruning that checks for this kind of error.
        resolvedYetStuckMethodCall.add(call.getNameAsString() + "@" + call.getArguments().size());
        return super.visit(call, p);
      } catch (RuntimeException e) {
        // Handle cases where a method call is resolved but its signature confuses JavaParser,
        // leading to a RuntimeException.
        // Note: this preservation is safe because we are not having an UnsolvedSymbolException.
        // Only unsolved symbols can make the output failed to compile.
        if (call.hasScope()) {
          Expression scope = call.getScope().orElseThrow();
          String scopeAsString = scope.toString();
          if (scopeAsString.equals("this") || scopeAsString.equals("super")) {
            // In the "super" case, it would be better to add the name of an
            // extended or implemented class/interface. However, there are two complications:
            // 1) we currently don't track the list of classes/interfaces that the current class
            // extends and/or implements in this visitor and 2) even if we did track that, there
            // is no way for us to know which of those classes/interfaces the method belongs to.
            // TODO: write a test for the "super" case and then figure out a better way to handle
            // it.
            resolvedYetStuckMethodCall.add(
                this.currentClassQualifiedName + "." + call.getNameAsString());
          } else {
            // Use the scope instead. First, check if it's resolvable. If it is, great -
            // just use that. If not, then we need to use some heuristics as fallbacks.
            try {
              ResolvedType scopeType = scope.calculateResolvedType();
              resolvedYetStuckMethodCall.add(scopeType.describe() + "." + call.getNameAsString());
              usedTypeElements.add(scopeType.describe());
            } catch (Exception e1) {
              // There are two fallback cases: the scope is an FQN (e.g., in
              // a call to a fully-qualified static method) or the scope is a simple name.
              // In the simple name case, append the current package to the front, since
              // if it had been imported we wouldn't be in this situation.
              if (UnsolvedSymbolVisitor.isAClassPath(scopeAsString)) {
                resolvedYetStuckMethodCall.add(scopeAsString + "." + call.getNameAsString());
                usedTypeElements.add(scopeAsString);
              } else {
                resolvedYetStuckMethodCall.add(
                    getCurrentPackage() + "." + scopeAsString + "." + call.getNameAsString());
                usedTypeElements.add(getCurrentPackage() + "." + scopeAsString);
              }
            }
          }
        } else {
          resolvedYetStuckMethodCall.add(
              this.currentClassQualifiedName + "." + call.getNameAsString());
        }
        return super.visit(call, p);
      }
      preserveMethodDecl(decl);
      // Special case for lambdas to preserve artificial functional
      // interfaces.
      for (int i = 0; i < call.getArguments().size(); ++i) {
        Expression arg = call.getArgument(i);
        if (arg.isLambdaExpr()) {
          updateUsedClassBasedOnType(decl.getParam(i).getType());
          // We should mark the abstract method for preservation as well
          if (decl.getParam(i).getType().isReferenceType()) {
            ResolvedReferenceType functionalInterface =
                decl.getParam(i).getType().asReferenceType();
            for (MethodUsage method : functionalInterface.getDeclaredMethods()) {
              if (method.getDeclaration().isAbstract()) {
                preserveMethodDecl(method.getDeclaration());
                // Only one abstract method per functional interface
                break;
              }
            }
          }
        }
      }
    }
    return super.visit(call, p);
  }

  /**
   * Helper method for preserving a used method. This code is called for both method call
   * expressions and method refs.
   *
   * @param decl a resolved method declaration to be preserved
   */
  private void preserveMethodDecl(ResolvedMethodDeclaration decl) {
    usedMembers.add(decl.getQualifiedSignature());
    updateUsedClassWithQualifiedClassName(
        decl.getPackageName() + "." + decl.getClassName(),
        usedTypeElements,
        nonPrimaryClassesToPrimaryClass);
    try {
      ResolvedType methodReturnType = decl.getReturnType();
      if (methodReturnType instanceof ResolvedReferenceType) {
        updateUsedClassBasedOnType(methodReturnType);
      }
    }
    // There could be two cases here:
    // 1) The return type is a completely generic type.
    // 2) UnsolvedSymbolVisitor has missed some unsolved symbols.
    catch (UnsolvedSymbolException e) {
      return;
    }

    for (int i = 0; i < decl.getNumberOfParams(); ++i) {
      // Why is there no getParams() method??
      ResolvedParameterDeclaration p = decl.getParam(i);
      ResolvedType pType = p.getType();
      updateUsedClassBasedOnType(pType);
    }
  }

  /**
   * Gets the package name of the current class.
   *
   * @return the current package name
   */
  private String getCurrentPackage() {
    return currentPackage;
  }

  @Override
  public Visitable visit(ClassOrInterfaceType type, Void p) {
    if (!insideTargetMember) {
      return super.visit(type, p);
    }
    try {
      ResolvedReferenceType typeResolved =
          JavaParserUtil.classOrInterfaceTypeToResolvedReferenceType(type);
      updateUsedClassBasedOnType(typeResolved);
    }
    // if the type has a fully-qualified form, JavaParser also consider other components rather than
    // the class name as ClassOrInterfaceType. For example, if the type is org.A.B, then JavaParser
    // will also consider org and org.A as ClassOrInterfaceType.
    // if type is a type variable, we will get an UnsupportedOperation Exception.
    catch (UnsolvedSymbolException | UnsupportedOperationException e) {
      return super.visit(type, p);
    }
    return super.visit(type, p);
  }

  @Override
  public Visitable visit(ObjectCreationExpr newExpr, Void p) {
    if (insideTargetMember) {
      try {
        ResolvedConstructorDeclaration resolved = newExpr.resolve();
        usedMembers.add(resolved.getQualifiedSignature());
        updateUsedClassWithQualifiedClassName(
            resolved.getPackageName() + "." + resolved.getClassName(),
            usedTypeElements,
            nonPrimaryClassesToPrimaryClass);
        for (int i = 0; i < resolved.getNumberOfParams(); ++i) {
          // Why is there no getParams() method??
          ResolvedParameterDeclaration param = resolved.getParam(i);
          ResolvedType pType = param.getType();
          updateUsedClassBasedOnType(pType);
        }
      } catch (UnsolvedSymbolException e) {
        throw new RuntimeException("trying to resolve : " + newExpr, e);
      }
    }
    return super.visit(newExpr, p);
  }

  @Override
  public Visitable visit(ExplicitConstructorInvocationStmt expr, Void p) {
    if (insideTargetMember) {
      ResolvedConstructorDeclaration resolved = expr.resolve();
      usedMembers.add(resolved.getQualifiedSignature());
      updateUsedClassWithQualifiedClassName(
          resolved.getPackageName() + "." + resolved.getClassName(),
          usedTypeElements,
          nonPrimaryClassesToPrimaryClass);
    }
    return super.visit(expr, p);
  }

  @Override
  public Visitable visit(EnumConstantDeclaration enumConstantDeclaration, Void p) {
    Node parentNode = enumConstantDeclaration.getParentNode().orElseThrow();

    if (parentNode instanceof EnumDeclaration) {
      if (usedTypeElements.contains(
          ((EnumDeclaration) parentNode)
              .asEnumDeclaration()
              .getFullyQualifiedName()
              .orElseThrow())) {
        boolean oldInsideTargetMember = insideTargetMember;
        // used enum constant are not strictly target methods, but we need to make sure the symbols
        // inside them are preserved.
        insideTargetMember = true;
        Visitable result = super.visit(enumConstantDeclaration, p);
        insideTargetMember = oldInsideTargetMember;

        return result;
      }
    }
    return super.visit(enumConstantDeclaration, p);
  }

  @Override
  public Visitable visit(FieldAccessExpr expr, Void p) {
    if (insideTargetMember) {
      String fullNameOfClass;
      if (updateUsedClassAndMemberForEnumConstant(expr)) {
        return super.visit(expr, p);
      }
      try {
        // while the name of the method is declaringType(), it actually returns the class where the
        // field is declared
        fullNameOfClass = expr.resolve().asField().declaringType().getQualifiedName();
        usedMembers.add(fullNameOfClass + "#" + expr.getName().asString());
        updateUsedClassWithQualifiedClassName(
            fullNameOfClass, usedTypeElements, nonPrimaryClassesToPrimaryClass);
        ResolvedType exprResolvedType = expr.resolve().getType();
        updateUsedClassBasedOnType(exprResolvedType);
      } catch (UnsolvedSymbolException | UnsupportedOperationException e) {
        // when the type is a primitive array, we will have an UnsupportedOperationException
        if (e instanceof UnsupportedOperationException) {
          Expression scope = expr.getScope();
          if (scope.isNameExpr()) {
            updateUsedElementWithPotentialFieldNameExpr(scope.asNameExpr());
          }
          // If the scope is not a name expression, then it must be "this" (handled elsewhere),
          // "super" (handled directly below), or another field access expression (handled by
          // the visitor), so there's nothing to do.
        }
        // if a field is accessed in the form of a fully-qualified path, such as
        // org.example.A.b, then other components in the path apart from the class name and field
        // name, such as org and org.example, will also be considered as FieldAccessExpr.
      }
    }
    Expression caller = expr.getScope();
    if (caller instanceof SuperExpr) {
      ResolvedType callerResolvedType = caller.calculateResolvedType();
      updateUsedClassBasedOnType(callerResolvedType);
    }
    return super.visit(expr, p);
  }

  @Override
  public Visitable visit(NameExpr expr, Void p) {
    if (insideTargetMember) {
      Optional<Node> parentNode = expr.getParentNode();
      if (parentNode.isEmpty() || !(parentNode.get() instanceof FieldAccessExpr)) {
        updateUsedElementWithPotentialFieldNameExpr(expr);
      }
    }
    return super.visit(expr, p);
  }

  /**
   * Updates the list of used classes based on a resolved method declaration. If the input method
   * originates from an interface, that interface will be added to the list of used classes. The
   * determination of whether a method belongs to an interface is based on three criteria: method
   * name, method return type, and the number of parameters.
   *
   * @param method The resolved method declaration to be used for updating the list.
   */
  public void updateUsedClassesForInterface(ResolvedMethodDeclaration method) {
    for (ResolvedMethodDeclaration interfaceMethod : methodDeclarationToInterfaceType.keySet()) {
      if (method.getName().equals(interfaceMethod.getName())) {
        try {
          if (method
              .getReturnType()
              .describe()
              .equals(interfaceMethod.getReturnType().describe())) {
            if (method.getNumberOfParams() == interfaceMethod.getNumberOfParams()) {
              ResolvedReferenceType resolvedInterface =
                  JavaParserUtil.classOrInterfaceTypeToResolvedReferenceType(
                      methodDeclarationToInterfaceType.get(interfaceMethod));
              updateUsedClassWithQualifiedClassName(
                  resolvedInterface.getQualifiedName(),
                  usedTypeElements,
                  nonPrimaryClassesToPrimaryClass);
              usedMembers.add(interfaceMethod.getQualifiedSignature());
            }
          }
        } catch (UnsolvedSymbolException e) {
          // only potentially-used members will have their symbols solved.
          continue;
        }
      }
    }
  }

  /**
   * Resolves unionType parameters one by one and adds them in the usedClass set.
   *
   * @param type unionType parameter
   */
  private void resolveUnionType(UnionType type) {
    for (ReferenceType param : type.getElements()) {
      ResolvedType paramType = param.resolve();
      updateUsedClassBasedOnType(paramType);
    }
  }

  /**
   * Given a FieldAccessExpr, this method updates the sets of used classes and members if this field
   * is actually an enum constant.
   *
   * @param fieldAccessExpr a potential enum constant.
   * @return true if the updating process was successful, false otherwise.
   */
  private boolean updateUsedClassAndMemberForEnumConstant(FieldAccessExpr fieldAccessExpr) {
    ResolvedValueDeclaration resolved;
    try {
      resolved = fieldAccessExpr.resolve();
    }
    // if the a field is accessed in the form of a fully-qualified path, such as
    // org.example.A.b, then other components in the path apart from the class name and field
    // name, such as org and org.example, will also be considered as FieldAccessExpr.
    catch (UnsolvedSymbolException | UnsupportedOperationException e) {
      return false;
    }
    if (!resolved.isEnumConstant()) {
      return false;
    }
    String classFullName = resolved.asEnumConstant().getType().describe();
    updateUsedClassWithQualifiedClassName(
        classFullName, usedTypeElements, nonPrimaryClassesToPrimaryClass);
    usedMembers.add(classFullName + "." + fieldAccessExpr.getNameAsString());
    return true;
  }

  /**
   * Given a NameExpr instance, this method will update the used elements, classes and members if
   * that NameExpr is a field.
   *
   * @param expr a field access expression inside target methods
   */
  public void updateUsedElementWithPotentialFieldNameExpr(NameExpr expr) {
    ResolvedValueDeclaration exprDecl;
    try {
      exprDecl = expr.resolve();
    } catch (UnsolvedSymbolException e) {
      // if expr is the name of a class in a static call, we can't resolve its value.
      return;
    }
    if (exprDecl instanceof ResolvedFieldDeclaration) {
      // while the name of the method is declaringType(), it actually returns the class where the
      // field is declared
      String classFullName = exprDecl.asField().declaringType().getQualifiedName();
      updateUsedClassWithQualifiedClassName(
          classFullName, usedTypeElements, nonPrimaryClassesToPrimaryClass);
      usedMembers.add(classFullName + "#" + expr.getNameAsString());
      updateUsedClassBasedOnType(exprDecl.getType());
    } else if (exprDecl instanceof ResolvedEnumConstantDeclaration) {
      String enumFullName = exprDecl.asEnumConstant().getType().describe();
      updateUsedClassWithQualifiedClassName(
          enumFullName, usedTypeElements, nonPrimaryClassesToPrimaryClass);
      // "." and not "#" because enum constants are not fields
      usedMembers.add(enumFullName + "." + expr.getNameAsString());
      updateUsedClassBasedOnType(exprDecl.getType());
    }
  }

  /**
   * Updates the list of used type elements with the given qualified type name and its corresponding
   * primary type and enclosing type. This includes cases such as classes not sharing the same name
   * as their Java files or nested classes.
   *
   * @param qualifiedClassName The qualified class name to be included in the list of used type
   *     elements.
   * @param usedTypeElement The set of used type elements to be updated.
   * @param nonPrimaryClassesToPrimaryClass Map connecting non-primary classes to their
   *     corresponding primary classes.
   */
  public static void updateUsedClassWithQualifiedClassName(
      String qualifiedClassName,
      Set<String> usedTypeElement,
      Map<String, String> nonPrimaryClassesToPrimaryClass) {

    // in case of type variables
    if (!qualifiedClassName.contains(".")) {
      return;
    }
    // strip type variables, if they're present
    if (qualifiedClassName.contains("<")) {
      qualifiedClassName = qualifiedClassName.substring(0, qualifiedClassName.indexOf("<"));
    }
    usedTypeElement.add(qualifiedClassName);
    // in case this class is not a primary class.
    if (nonPrimaryClassesToPrimaryClass.containsKey(qualifiedClassName)) {
      updateUsedClassWithQualifiedClassName(
          nonPrimaryClassesToPrimaryClass.get(qualifiedClassName),
          usedTypeElement,
          nonPrimaryClassesToPrimaryClass);
    }

    String potentialOuterClass =
        qualifiedClassName.substring(0, qualifiedClassName.lastIndexOf("."));
    if (UnsolvedSymbolVisitor.isAClassPath(potentialOuterClass)) {
      updateUsedClassWithQualifiedClassName(
          potentialOuterClass, usedTypeElement, nonPrimaryClassesToPrimaryClass);
    }
  }

  /**
   * Updates the list of used classes based on the resolved type of a used element, where a element
   * can be a method, a field, a variable, or a parameter. Also updates the set of used classes
   * based on component types, wildcard bounds, etc., as needed: any type that is used in the type
   * will be included.
   *
   * @param type The resolved type of the used element.
   */
  public void updateUsedClassBasedOnType(ResolvedType type) {
    if (type.isTypeVariable()) {
      // From JLS 4.4: A type variable is introduced by the declaration of a type parameter of a
      // generic class, interface, method, or constructor
      ResolvedTypeParameterDeclaration asTypeParameter = type.asTypeParameter();
      for (ResolvedTypeParameterDeclaration.Bound bound : asTypeParameter.getBounds()) {
        updateUsedClassWithQualifiedClassName(
            bound.getType().describe(), usedTypeElements, nonPrimaryClassesToPrimaryClass);
      }
      return;
    } else if (type.isArray()) {
      ResolvedType componentType = type.asArrayType().getComponentType();
      updateUsedClassBasedOnType(componentType);
      return;
    }
    updateUsedClassWithQualifiedClassName(
        type.describe(), usedTypeElements, nonPrimaryClassesToPrimaryClass);
    if (!type.isReferenceType()) {
      return;
    }
    ResolvedReferenceType typeAsReference = type.asReferenceType();
    List<ResolvedType> typeParameters = typeAsReference.typeParametersValues();
    for (ResolvedType typePara : typeParameters) {
      if (typePara.isPrimitive() || typePara.isTypeVariable()) {
        // Nothing to do, since these are already in-scope.
        continue;
      }
      if (typePara.isWildcard()) {
        ResolvedWildcard asWildcard = typePara.asWildcard();
        // Recurse into the bound, if one exists.
        if (asWildcard.isBounded()) {
          updateUsedClassBasedOnType(asWildcard.getBoundedType());
        }
        continue;
      }
      updateUsedClassWithQualifiedClassName(
          typePara.asReferenceType().getQualifiedName(),
          usedTypeElements,
          nonPrimaryClassesToPrimaryClass);
    }
  }
}
