package org.checkerframework.specimin;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedFieldDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The main visitor for Specimin's first phase, which locates the target method(s) and compiles
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
   * The members (methods and fields) that were actually used by the targets, and therefore ought to
   * have their specifications (but not bodies) preserved. The Strings in the set are the
   * fully-qualified names, as returned by ResolvedMethodDeclaration#getQualifiedSignature for
   * methods and FieldAccessExpr#getName for fields.
   */
  private final Set<String> usedMembers = new HashSet<>();

  /**
   * Classes of the methods that were actually used by the targets. These classes will be included
   * in the input.
   */
  private Set<String> usedClass = new HashSet<>();

  /** Set of variables declared in this current class */
  private final Set<String> declaredNames = new HashSet<>();

  /**
   * The resolved target methods. The Strings in the set are the fully-qualified names, as returned
   * by ResolvedMethodDeclaration#getQualifiedSignature.
   */
  private final Set<String> targetMethods = new HashSet<>();

  /**
   * A local copy of the input list of methods. A method is removed from this copy when it is
   * located. If the visitor has been run on all source files and this list isn't empty, that
   * usually indicates an error.
   */
  private final List<String> unfoundMethods;

  /**
   * Create a new target method finding visitor.
   *
   * @param methodNames the names of the target methods, the format
   *     class.fully.qualified.Name#methodName(Param1Type, Param2Type, ...)
   */
  public TargetMethodFinderVisitor(List<String> methodNames) {
    targetMethodNames = new HashSet<>();
    targetMethodNames.addAll(methodNames);
    unfoundMethods = new ArrayList<>(methodNames);
  }

  /**
   * Returns the methods that so far this visitor has not located from its target list. Usually,
   * this should be checked after running the visitor to ensure that it is empty.
   *
   * @return the methods that so far this visitor has not located from its target list
   */
  public List<String> getUnfoundMethods() {
    return unfoundMethods;
  }

  /**
   * Get the methods that this visitor has concluded that the target method(s) use, and therefore
   * ought to be retained. The Strings in the set are the fully-qualified names, as returned by
   * ResolvedMethodDeclaration#getQualifiedSignature.
   *
   * @return the used methods
   */
  public Set<String> getUsedMembers() {
    return usedMembers;
  }

  /**
   * Get the classes of the methods that the target method uses. The Strings in the set are the
   * fully-qualified names.
   *
   * @return the used classes
   */
  public Set<String> getUsedClass() {
    return usedClass;
  }

  /**
   * Get the target methods that this visitor has encountered so far. The Strings in the set are the
   * fully-qualified names, as returned by ResolvedMethodDeclaration#getQualifiedSignature.
   *
   * @return the target methods
   */
  public Set<String> getTargetMethods() {
    return targetMethods;
  }

  @Override
  public Visitable visit(ClassOrInterfaceDeclaration decl, Void p) {
    if (decl.isNestedType()) {
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
    if (decl.isNestedType()) {
      this.classFQName = this.classFQName.substring(0, this.classFQName.lastIndexOf('.'));
    } else {
      this.classFQName = "";
    }
    return result;
  }

  @Override
  public Visitable visit(ConstructorDeclaration method, Void p) {
    String constructorMethodAsString = method.getDeclarationAsString(false, false, false);
    // the methodName will be something like this: "com.example.Car#Car()"
    String methodName = this.classFQName + "#" + constructorMethodAsString;
    if (this.targetMethodNames.contains(methodName)) {
      insideTargetMethod = true;
      targetMethods.add(method.resolve().getQualifiedSignature());
      unfoundMethods.remove(methodName);
    }
    Visitable result = super.visit(method, p);
    insideTargetMethod = false;
    return result;
  }

  @Override
  public Visitable visit(VariableDeclarator node, Void arg) {
    declaredNames.add(node.getNameAsString());
    return super.visit(node, arg);
  }

  @Override
  public Visitable visit(MethodDeclaration method, Void p) {
    String methodDeclAsString = method.getDeclarationAsString(false, false, false);
    // The substring here is to remove the method's return type. Return types cannot contain spaces.
    // TODO: test this with annotations
    String methodName =
        this.classFQName + "#" + methodDeclAsString.substring(methodDeclAsString.indexOf(' ') + 1);
    // this method belongs to an anonymous class inside the target method
    if (insideTargetMethod) {
      ObjectCreationExpr parentExpression = (ObjectCreationExpr) method.getParentNode().get();
      ResolvedConstructorDeclaration resolved = parentExpression.resolve();
      String methodPackage = resolved.getPackageName();
      String methodClass = resolved.getClassName();
      usedMembers.add(methodPackage + "." + methodClass + "." + method.getNameAsString() + "()");
      usedClass.add(methodPackage + "." + methodClass);
    }

    if (this.targetMethodNames.contains(methodName)) {
      insideTargetMethod = true;
      targetMethods.add(method.resolve().getQualifiedSignature());
      unfoundMethods.remove(methodName);
      Type returnType = method.getType();
      // JavaParser may misinterpret unresolved array types as reference types.
      // To ensure accuracy, we resolve the type before proceeding with the check.
      try {
        ResolvedType resolvedType = returnType.resolve();
        if (resolvedType instanceof ResolvedReferenceType) {
          usedClass.add(resolvedType.asReferenceType().getQualifiedName());
        }
      } catch (UnsupportedOperationException e) {
        // Occurs if the type is a type variable, so there is nothing to do:
        // the type variable must have been declared in one of the containing scopes,
        // and UnsolvedSymbolVisitor should already guarantee that the variable will
        // be included in one of the classes that Specimin outputs.
      }
    }
    Visitable result = super.visit(method, p);
    insideTargetMethod = false;
    return result;
  }

  @Override
  public Visitable visit(Parameter para, Void p) {
    if (insideTargetMethod) {
      Type type = para.getType();
      String paraTypeFullName = "";
      ResolvedType paramType;
      if (type.isUnionType()) {
        for (var param : para.getType().asUnionType().getElements()) {
          try {
            paramType = param.resolve();
            paraTypeFullName =
                paramType.asReferenceType().getTypeDeclaration().get().getQualifiedName();
            usedClass.add(paraTypeFullName);
          } catch (
              Exception
                  e) { // This visitor should not have an unsolvable parameter type. Throwing if
            // exception occurs.
            throw new Error(e.getMessage());
          }
        }
      } else {
        paramType = para.resolve().getType();
        if (paramType.isReferenceType()) {
          paraTypeFullName =
              paramType.asReferenceType().getTypeDeclaration().get().getQualifiedName();
          usedClass.add(paraTypeFullName);
          for (ResolvedType typeParameterValue :
              paramType.asReferenceType().typeParametersValues()) {
            String typeParameterValueName = typeParameterValue.describe();
            if (typeParameterValueName.contains("<")) {
              // removing the "<...>" part if there is any.
              typeParameterValueName =
                  typeParameterValueName.substring(0, typeParameterValueName.indexOf("<"));
            }
            usedClass.add(typeParameterValueName);
          }
        }
      }
    }
    return super.visit(para, p);
  }

  @Override
  public Visitable visit(MethodCallExpr call, Void p) {
    if (insideTargetMethod) {
      usedMembers.add(call.resolve().getQualifiedSignature());
      usedClass.add(call.resolve().getPackageName() + "." + call.resolve().getClassName());
      ResolvedType methodReturnType = call.resolve().getReturnType();
      if (methodReturnType instanceof ResolvedReferenceType) {
        usedClass.add(methodReturnType.asReferenceType().getQualifiedName());
      }
    }
    return super.visit(call, p);
  }

  @Override
  public Visitable visit(ObjectCreationExpr newExpr, Void p) {
    if (insideTargetMethod) {
      usedMembers.add(newExpr.resolve().getQualifiedSignature());
      usedClass.add(newExpr.resolve().getPackageName() + "." + newExpr.resolve().getClassName());
    }
    return super.visit(newExpr, p);
  }

  @Override
  public Visitable visit(ExplicitConstructorInvocationStmt expr, Void p) {
    if (insideTargetMethod) {
      usedMembers.add(expr.resolve().getQualifiedSignature());
      usedClass.add(expr.resolve().getPackageName() + "." + expr.resolve().getClassName());
    }
    return super.visit(expr, p);
  }

  @Override
  public Visitable visit(FieldAccessExpr expr, Void p) {
    if (insideTargetMethod) {
      usedMembers.add(classFQName + "#" + expr.getName().asString());
    }
    Expression caller = expr.getScope();
    if (caller instanceof SuperExpr) {
      usedClass.add(caller.calculateResolvedType().describe());
    }
    return super.visit(expr, p);
  }

  @Override
  public Visitable visit(NameExpr expr, Void p) {
    Optional<Node> parentNode = expr.getParentNode();
    if (parentNode.isEmpty()
        || !(parentNode.get() instanceof MethodCallExpr
            || parentNode.get() instanceof FieldAccessExpr)) {
      ResolvedValueDeclaration exprDecl = expr.resolve();
      if (exprDecl instanceof ResolvedFieldDeclaration) {
        usedClass.add(exprDecl.asField().declaringType().getQualifiedName());
      }
    }
    return super.visit(expr, p);
  }
}
