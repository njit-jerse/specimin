package org.checkerframework.specimin;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.nodeTypes.NodeWithDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.nodeTypes.NodeWithTraversableScope;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedEnumConstantDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration.Bound;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedLambdaConstraintType;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.resolution.types.ResolvedWildcard;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFieldDeclaration;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration;
import com.github.javaparser.utils.Pair;
import com.google.common.base.Splitter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.specimin.unsolved.SolvedMemberType;

/**
 * A class containing useful static functions using JavaParser.
 *
 * <p>This class cannot be instantiated.
 */
public class JavaParserUtil {

  /**
   * Private constructor to prevent instantiation.
   *
   * @throws UnsupportedOperationException if an attempt is made to instantiate this class.
   */
  private JavaParserUtil() {
    throw new UnsupportedOperationException("This class cannot be instantiated.");
  }

  /**
   * Set this SpeciminTypeSolvers instance to be used by this class, so we don't have to find type
   * solvers through reflection in JavaParserSymbolSolver. This field is initialized once in
   * SpeciminRunner. Note that by the time you evaluate the value of this field, it should already
   * be non-null.
   */
  private static @MonotonicNonNull SpeciminTypeSolvers typeSolvers = null;

  /**
   * Keeps track of the placeholder types that have been generated and registered with the type
   * solver in {@link #getResolvablePlaceholderType(int)}.
   */
  private static final Set<String> generatedResolvedPlaceholderTypes = new HashSet<>();

  /**
   * Set the SpeciminTypeSolvers instance to be used.
   *
   * @param typeSolvers The SpeciminTypeSolvers instance holding all type solvers.
   */
  @EnsuresNonNull("JavaParserUtil.typeSolvers")
  public static void setTypeSolvers(SpeciminTypeSolvers typeSolvers) {
    JavaParserUtil.typeSolvers = typeSolvers;

    // If we reset memoryTypeSolver, we should clear our cache since these old types
    // are not registered with the new MemoryTypeSolver. This is mainly an issue with
    // the testing suite since JavaParserUtil is static and is used across different
    // test cases.
    generatedResolvedPlaceholderTypes.clear();
  }

  /**
   * Gets the type solver, and ensures it is non-null.
   *
   * @return The type solver
   */
  public static SpeciminTypeSolvers getTypeSolvers() {
    if (typeSolvers == null) {
      throw new RuntimeException(
          "typeSolvers is not set. Make sure to call setTypeSolvers() in SpeciminRunner.");
    }
    return typeSolvers;
  }

  /**
   * This method checks if a string has the form of a class path.
   *
   * @param potentialClassPath the string to be checked
   * @return true if the string is a class path
   */
  public static boolean isAClassPath(String potentialClassPath) {
    List<String> elements = Splitter.onPattern("\\.").splitToList(potentialClassPath);
    int elementsCount = elements.size();
    return elementsCount > 1
        && isAClassName(elements.get(elementsCount - 1))
        // Classpaths cannot contain spaces!
        && elements.stream().noneMatch(s -> s.contains(" "));
  }

  /**
   * This method checks if a string represents a simple class name.
   *
   * @param string the string to be checked
   * @return true if the string represents a simple class name
   */
  public static boolean isAClassName(String string) {
    if (string.contains(".")) {
      return false;
    }

    // A class name should have its first letter capitalized but its second letter
    // should be lowercase. If otherwise, then it may be a constant
    char first = string.charAt(0);
    if (string.length() > 1) {
      char second = string.charAt(1);

      return Character.isUpperCase(first) && Character.isLowerCase(second);
    }

    // A name like "A": assume it's a class
    return Character.isUpperCase(first);
  }

  /**
   * Returns true if a simple name looks like a constant; i.e., all its characters are either
   * capital or _. This is a heuristic.
   *
   * @param simpleName The simple name to check, should contain no dots
   * @return If it looks like a constant
   */
  public static boolean looksLikeAConstant(String simpleName) {
    for (int i = 0; i < simpleName.length(); i++) {
      char character = simpleName.charAt(i);
      if (!Character.isUpperCase(character) && character != '_') {
        return false;
      }
    }

    return true;
  }

  /**
   * Utility method to check if the given declaration is a local class declaration.
   *
   * @param decl a class, interface, or enum declaration
   * @return true iff the given declaration is of a local class
   */
  public static boolean isLocalClassDecl(TypeDeclaration<?> decl) {
    return decl.isClassOrInterfaceDeclaration()
        && decl.asClassOrInterfaceDeclaration().isLocalClassDeclaration();
  }

  /**
   * Erases type arguments from a method or type signature string.
   *
   * @param signature the signature
   * @return the same signature without type arguments
   */
  public static String erase(String signature) {
    return signature.replaceAll("<.*>", "");
  }

  /**
   * Returns the innermost enclosing class-like element for the given node. A class-like element is
   * a class, interface, or enum (i.e., something that would be a {@link
   * javax.lang.model.element.TypeElement} in javac's internal model). This method will throw if no
   * such element exists.
   *
   * @param node a node that is contained in a class-like structure
   * @return the nearest enclosing class-like node
   */
  public static TypeDeclaration<?> getEnclosingClassLike(Node node) {
    Node parent = node.getParentNode().orElseThrow();
    while (!(parent instanceof TypeDeclaration<?>)) {
      parent = parent.getParentNode().orElseThrow();
    }
    return (TypeDeclaration<?>) parent;
  }

  /**
   * See {@link #getEnclosingClassLike(Node)} for more details. This does not throw if a parent is
   * not found; instead, it returns null.
   *
   * @param node a node that is contained in a class-like structure
   * @return the nearest enclosing class-like node
   */
  public static @Nullable TypeDeclaration<?> getEnclosingClassLikeOptional(Node node) {
    try {
      return getEnclosingClassLike(node);
    } catch (Exception ex) {
      return null;
    }
  }

  /**
   * Gets the super class of a node. If one does not exist, this method will throw.
   *
   * @param node The node to find the super class of
   * @return The super class
   */
  public static ClassOrInterfaceType getSuperClass(Node node) {
    TypeDeclaration<?> decl = getEnclosingClassLike(node);

    return decl.asClassOrInterfaceDeclaration().getExtendedTypes().get(0);
  }

  /**
   * Given a qualified class name, return the simple name. i.e., org.example.ClassName -->
   * ClassName.
   *
   * <p>This is also safe to call on nonqualified names as well; it simply returns the input.
   *
   * @param qualified The qualified class name
   * @return The simple class name
   */
  public static String getSimpleNameFromQualifiedName(String qualified) {
    return qualified.substring(qualified.lastIndexOf('.') + 1);
  }

  /**
   * Given a method or constructor declaration, this method returns the declaration of that method
   * without the return type and any possible annotation. Note: the result may have spaces!
   *
   * @param decl the declaration to be used as input
   * @return decl without the return type and any possible annotation.
   */
  public static String removeMethodReturnTypeAndAnnotations(NodeWithDeclaration decl) {
    String declAsString = decl.getDeclarationAsString(false, false, false);
    return removeMethodReturnTypeAndAnnotationsImpl(declAsString);
  }

  /**
   * Implementation of {@link #removeMethodReturnTypeAndAnnotations(NodeWithDeclaration)}. Separated
   * for testing.
   *
   * @param declAsString the string form of a declaration
   * @return the string without the return type and the annotations
   */
  static String removeMethodReturnTypeAndAnnotationsImpl(String declAsString) {
    List<String> methodParts = Splitter.onPattern(" ").splitToList(declAsString);
    // remove all annotations
    String filteredMethodDeclaration =
        methodParts.stream()
            .filter(part -> !part.startsWith("@"))
            .map(part -> part.indexOf('@') == -1 ? part : part.substring(0, part.indexOf('@')))
            .collect(Collectors.joining(" "));
    // remove everything but the name and the return type
    methodParts =
        Splitter.onPattern(" ")
            .splitToList(
                filteredMethodDeclaration.substring(0, filteredMethodDeclaration.indexOf('(')));
    String methodName = methodParts.get(methodParts.size() - 1);
    String methodReturnType =
        filteredMethodDeclaration.substring(0, filteredMethodDeclaration.indexOf(methodName));
    String methodWithoutReturnType = filteredMethodDeclaration.replace(methodReturnType, "");
    // sometimes an extra space may occur if an annotation right after a < was removed
    return methodWithoutReturnType.replace("< ", "<");
  }

  /**
   * Returns the FQN if the expression is a reference to a static method or field or null if it
   * isn't one. This method is intended to be used with unsolvable expressions, with which it should
   * always return the correct result.
   *
   * @param expr The expression
   * @return The FQN if it is a static member, empty otherwise
   */
  public static @Nullable String getFQNIfStaticMember(Expression expr) {
    CompilationUnit cu = expr.findCompilationUnit().get();

    String nameOfExpr;
    String nameOfScope = null;
    Expression scope = null;

    if (expr.isNameExpr()) {
      nameOfScope = expr.asNameExpr().getNameAsString();
      nameOfExpr = expr.asNameExpr().getNameAsString();

      // If an expression is a class name, it cannot be a static member
      if (isAClassName(nameOfExpr)) {
        return null;
      }
    } else if (expr.isMethodCallExpr()) {
      nameOfExpr = expr.asMethodCallExpr().getNameAsString();
      if (expr.asMethodCallExpr().hasScope()) {
        scope = expr.asMethodCallExpr().getScope().get();
      } else {
        nameOfScope = nameOfExpr;
      }
    } else if (expr.isFieldAccessExpr()) {
      nameOfExpr = expr.asFieldAccessExpr().getNameAsString();
      scope = expr.asFieldAccessExpr().getScope();
    } else {
      return null;
    }

    if (scope != null) {
      if (scope.isNameExpr()) {
        nameOfScope = scope.asNameExpr().getNameAsString();
      } else if (scope.isFieldAccessExpr()) {
        nameOfScope = scope.asFieldAccessExpr().toString();
        if (isAClassPath(nameOfScope)) {
          return nameOfScope + "." + nameOfExpr;
        }
      } else {
        return null;
      }

      ResolvedType scopeType = Resolver.calculateResolvedType(scope);
      if (scopeType != null
          && getSimpleNameFromQualifiedName(scopeType.describe()).endsWith(scope.toString())) {
        return scopeType.describe() + "." + nameOfExpr;
      }
    }

    if (nameOfScope == null) {
      return null;
    }

    for (ImportDeclaration importDecl : cu.getImports()) {
      // A static member can either be imported as a static method/field, like
      // import static org.example.SomeClass.fieldName;
      if (importDecl.isStatic() && importDecl.getNameAsString().endsWith("." + nameOfExpr)) {
        return importDecl.getNameAsString();
      }

      // A static member can also be found if its scope is a non-static, imported type,
      // i.e., Foo.myField, if there is also import org.example.Foo;
      if (!importDecl.isStatic() && importDecl.getNameAsString().endsWith("." + nameOfScope)) {
        return expr.isNameExpr()
            ? importDecl.getNameAsString()
            : importDecl.getNameAsString() + "." + nameOfExpr;
      }
    }

    // The scope may also be a simple class name located in the same package
    if (scope != null && isAClassName(nameOfScope)) {
      return cu.getPackageDeclaration().get().getNameAsString()
          + "."
          + nameOfScope
          + "."
          + nameOfExpr;
    }

    return null;
  }

  /**
   * Gets the type from a {@code ResolvedValueDeclaration}. Returns null if unable to be found.
   *
   * @param resolved The resolved value declaration
   * @param fqnToCompilationUnits The map of FQNs to compilation units
   * @return The Type of the resolved value declaration
   */
  public static @Nullable Type getTypeFromResolvedValueDeclaration(
      ResolvedValueDeclaration resolved, Map<String, CompilationUnit> fqnToCompilationUnits) {
    Node attached = tryFindAttachedNode(resolved, fqnToCompilationUnits);

    if (attached instanceof VariableDeclarationExpr varDecl) {
      Type type = varDecl.getElementType();

      if (!type.isVarType()) {
        return type;
      }

      // var can only have one variable
      return tryGetTypeFromExpression(
          varDecl.getVariables().get(0).getInitializer().get(), fqnToCompilationUnits);
    } else if (attached instanceof VariableDeclarator varDecl) {
      Type type = varDecl.getType();

      if (!type.isVarType()) {
        return type;
      }

      // var can only have one variable
      return tryGetTypeFromExpression(varDecl.getInitializer().get(), fqnToCompilationUnits);
    } else if (attached instanceof FieldDeclaration fieldDecl) {
      return fieldDecl.getElementType();
    } else if (attached instanceof Parameter param) {
      return param.getType();
    } else if (attached instanceof AnnotationMemberDeclaration annoMemberDecl) {
      return annoMemberDecl.getType();
    }

    return null;
  }

  /**
   * Tries to get the type from an expression; useful for var types. If a type cannot be found, this
   * method returns null.
   *
   * @param expression The expression to get the type from
   * @return The type of the expression, or null if it cannot be found
   */
  private static @Nullable Type tryGetTypeFromExpression(
      Expression expression, Map<String, CompilationUnit> fqnToCompilationUnits) {
    Object resolved = null;

    if (expression.isNameExpr()) {
      resolved = Resolver.resolve(expression.asNameExpr());
    } else if (expression.isFieldAccessExpr()) {
      resolved = Resolver.resolve(expression.asFieldAccessExpr());
    } else if (expression.isMethodCallExpr()) {
      resolved = Resolver.resolve(expression.asMethodCallExpr());
    }

    if (resolved instanceof ResolvedValueDeclaration valueDecl) {
      return getTypeFromResolvedValueDeclaration(valueDecl, fqnToCompilationUnits);
    } else if (resolved instanceof ResolvedMethodDeclaration resolvedMethodDecl) {
      if (resolvedMethodDecl.toAst().isPresent()) {
        MethodDeclaration methodDecl = (MethodDeclaration) resolvedMethodDecl.toAst().get();
        return methodDecl.getType();
      }
    }

    if (expression.isArrayCreationExpr()) {
      return expression.asArrayCreationExpr().createdType();
    } else if (expression.isCastExpr()) {
      return expression.asCastExpr().getType();
    }

    return null;
  }

  /**
   * Tries to get the type from an expression; useful for var types. If a type cannot be found, this
   * method returns null. This method is similar to {@link #tryGetTypeFromExpression(Expression,
   * Map)}, but this accounts for slightly more cases when a String version of the type can be
   * returned, but a corresponding Type in the AST cannot be found.
   *
   * @param expression The expression to get the type from
   * @param fqnToCompilationUnits The map of FQNs to compilation units
   * @return The type of the expression, or null if it cannot be found
   */
  public static @Nullable String tryGetTypeAsStringFromExpression(
      Expression expression, Map<String, CompilationUnit> fqnToCompilationUnits) {
    ResolvedType scopeType = Resolver.calculateResolvedType(expression);
    if (scopeType != null) {
      return scopeType.describe();
    }

    Type type = tryGetTypeFromExpression(expression, fqnToCompilationUnits);

    if (type != null) {
      return type.toString();
    }

    if (expression.isClassExpr()) {
      return "java.lang.Class<" + expression.asClassExpr().getTypeAsString() + ">";
    } else if (expression.isInstanceOfExpr()) {
      return "boolean";
    }

    return null;
  }

  /**
   * Tries to get the expression type from an expression with an unresolvable scope. This is done by
   * replacing all unresolvable type arguments in the scope with resolvable placeholder types, and
   * then running {@code calculateResolvedType()}. If the expression can be resolved after this
   * process, its resolved type is returned (alongside a map with the FQNs of placeholder types to
   * their corresponding nodes--either Type or Expression). Otherwise, null is returned. Note that
   * if this method returns null, the AST will be unchanged; if this method returns a non-null
   * value, any changes made to the AST will have been reverted.
   *
   * @param expr The expression whose type is to be resolved
   * @param fqnToCompilationUnits The map of FQNs to compilation units
   * @return The resolved type, or null if it cannot be resolved
   */
  public static @Nullable Pair<ResolvedType, Map<String, Node>>
      tryGetExpressionTypeFromUnresolvableGenericScopeOrUnsolvedLambdas(
          Expression expr, Map<String, CompilationUnit> fqnToCompilationUnits) {

    Expression copy = expr.clone();
    Pair<Map<Type, Node>, IdentityHashMap<Node, Node>> copyAndPlaceholderMap =
        copyAndReplaceAllUnresolvableScopeTypeArgumentsAndLambdasWithResolvablePlaceholders(
            expr, copy, fqnToCompilationUnits);

    if (copyAndPlaceholderMap == null) {
      return null;
    }

    Map<Type, Node> placeholderTypeToTypeHolding = copyAndPlaceholderMap.a;
    IdentityHashMap<Node, Node> originalToCopies = copyAndPlaceholderMap.b;

    originalToCopies.put(expr, copy);
    return collectResolvedTypeAndPlaceholderMappingAndRevertChanges(
        expr, copy, placeholderTypeToTypeHolding, originalToCopies);
  }

  /**
   * Similar to {@link #tryGetExpressionTypeFromUnresolvableGenericScopeOrUnsolvedLambdas}, but this
   * is specifically for uses of lambda parameters in unresolvable generic scopes. For example,
   * resolving x.a() in foo.bar(x -> x.a()) when foo's type is solvable, but its type arguments are
   * not.
   *
   * @param expr The expression whose type is to be resolved
   * @param fqnToCompilationUnits The map of FQNs to compilation units
   * @return The resolved type, or null if it cannot be resolved
   */
  public static @Nullable Pair<ResolvedType, Map<String, Node>>
      tryGetExpressionTypeForLambdaParameterInUnresolvableGenericScopeMethod(
          Expression expr, Map<String, CompilationUnit> fqnToCompilationUnits) {

    @SuppressWarnings("unchecked")
    LambdaExpr lambda = expr.findAncestor(LambdaExpr.class).orElse(null);
    if (lambda == null) {
      return null;
    }

    @SuppressWarnings("unchecked")
    MethodCallExpr methodCall = lambda.findAncestor(MethodCallExpr.class).orElse(null);
    if (methodCall == null) {
      return null;
    }

    MethodCallExpr copyOfEnclosingMethodCall = methodCall.clone();
    Pair<Map<Type, Node>, IdentityHashMap<Node, Node>> copyAndPlaceholderMap =
        copyAndReplaceAllUnresolvableScopeTypeArgumentsAndLambdasWithResolvablePlaceholders(
            methodCall, copyOfEnclosingMethodCall, fqnToCompilationUnits);

    if (copyAndPlaceholderMap == null) {
      return null;
    }

    Map<Type, Node> placeholderToTypeHolding = copyAndPlaceholderMap.a;
    IdentityHashMap<Node, Node> originalToCopies = copyAndPlaceholderMap.b;

    // Take this code: foo.bar(x -> x.method()); copy is the copy of this whole method call, but we
    // are trying to resolve the type of x.method() (which is a copy of the parameter "expr"), so
    // we need to find the corresponding copy in copyOfEnclosingMethodCall
    Expression copyOfOriginalExpr =
        copyOfEnclosingMethodCall.findFirst(Expression.class, e -> e.equals(expr)).orElseThrow();

    // We need to do this since the below method call will replace copyOfOriginalExpr with expr,
    // but methodCall is what was cloned, so that is what we actually have to replace.
    originalToCopies.put(methodCall, copyOfEnclosingMethodCall);

    return collectResolvedTypeAndPlaceholderMappingAndRevertChanges(
        methodCall, copyOfOriginalExpr, placeholderToTypeHolding, originalToCopies);
  }

  /**
   * Helper method for the above two methods to collect the resolved type and placeholder mapping,
   * and then revert changes to the AST. This is separated out since the logic is the same for both
   * methods.
   *
   * @param originalExpressionBeforeClone the original expression that was cloned; not necessarily
   *     the same as the real version of clonedExpressionToEvaluate
   * @param clonedExpressionToEvaluate the cloned expression whose type we should evaluate
   * @param placeholderToTypeHolding the map from the placeholder types to the nodes that are
   *     holding the original types/lambda return expressions. Ensure that the original expression
   *     is included in this map with its corresponding copy.
   * @param originalToCopies the map from original nodes to their copies, used to revert changes to
   *     the AST
   * @return a pair of the resolved type and a map from placeholder type FQNs to their corresponding
   *     original nodes, or null if the type cannot be resolved
   */
  private static @Nullable Pair<ResolvedType, Map<String, Node>>
      collectResolvedTypeAndPlaceholderMappingAndRevertChanges(
          Expression originalExpressionBeforeClone,
          Expression clonedExpressionToEvaluate,
          Map<Type, Node> placeholderToTypeHolding,
          IdentityHashMap<Node, Node> originalToCopies) {
    @Nullable ResolvedType resolvedType =
        Resolver.calculateResolvedType(clonedExpressionToEvaluate);

    Map<String, Node> placeholderFQNToOriginalNode;

    placeholderFQNToOriginalNode =
        placeholderToTypeHolding.entrySet().stream()
            // If we're here, everything is resolvable
            .collect(
                Collectors.toMap(
                    entry -> Resolver.resolveGuaranteeNonNull(entry.getKey()).describe(),
                    Map.Entry::getValue,
                    (a, b) -> {
                      throw new IllegalStateException("Duplicate key");
                    },
                    HashMap::new));

    // Revert changes to the AST
    for (Map.Entry<Node, Node> entry : originalToCopies.entrySet()) {
      entry.getValue().replace(entry.getKey());
    }

    if (resolvedType == null) {
      return null;
    }

    Iterator<String> iterator = placeholderFQNToOriginalNode.keySet().iterator();
    while (iterator.hasNext()) {
      String placeholderType = iterator.next();
      Node originalNode = placeholderFQNToOriginalNode.get(placeholderType);

      if (originalNode == null) {
        throw new RuntimeException("cannot be non-null: satisfy null checker");
      }

      if (originalNode.findCompilationUnit().isPresent()) {
        continue;
      }

      // There's a chance that the "original" node is from a copy of the initial expression
      // (and thus not in a compilation unit), so we need to fix those. This is only a problem
      // for lambda arguments, not for scopes.

      placeholderFQNToOriginalNode.put(
          placeholderType,
          originalExpressionBeforeClone
              .findFirst(Node.class, n -> n.equals(originalNode))
              .orElseThrow());
    }

    return new Pair<>(resolvedType, placeholderFQNToOriginalNode);
  }

  /**
   * Copies the given expression and replaces all unresolvable type arguments in its scope. The AST
   * is modified after this method is run. {@code a} in the returned pair is a map of placeholder
   * types back to the original types (placeholder types to either a Type or Expression representing
   * a lambda return), and {@code b} should be used to replace modified nodes to their originals.
   *
   * @param originalExpr The expression to copy and replace unresolvable type arguments in the scope
   *     of
   * @param copyOfExpr A clone of originalExpr
   * @param fqnToCompilationUnits The map of FQNs to compilation units
   * @return A pair of a map from the placeholder types to the original types/lambda return expr and
   *     a map of original to modified nodes, or null if any unresolvable erased type is found in
   *     the scope
   */
  private static @Nullable Pair<Map<Type, Node>, IdentityHashMap<Node, Node>>
      copyAndReplaceAllUnresolvableScopeTypeArgumentsAndLambdasWithResolvablePlaceholders(
          Expression originalExpr,
          Expression copyOfExpr,
          Map<String, CompilationUnit> fqnToCompilationUnits) {

    if (!copyOfExpr.hasScope()) {
      // Unresolvable for some reason, and we have no scope to try to fix it, so just give up
      return null;
    }

    // Use a clone; JavaParser symbol resolution seems to have some internal cache, so we
    // don't want to mess with the original expression to avoid revealing placeholder types
    // in resolve() calls on the same expression outside of this method
    originalExpr.replace(copyOfExpr);

    Expression scope = ((NodeWithTraversableScope) copyOfExpr).traverseScope().get();

    Pair<Map<Type, Node>, IdentityHashMap<Node, Node>> placeholderToOriginalAndCopies =
        replaceAllUnresolvableScopeTypeArgumentsWithResolvablePlaceholders(
            scope, fqnToCompilationUnits);

    if (placeholderToOriginalAndCopies == null) {
      copyOfExpr.replace(originalExpr);
    }
    return placeholderToOriginalAndCopies;
  }

  /**
   * Replaces all unresolvable type arguments in the scope or unsolved lambda arguments in a method
   * call with resolvable placeholder types, and stores the mapping from the placeholder types to
   * the original types/lambda return expressions in the result map. If any part of the scope has an
   * erased type that cannot be resolved, this method returns null and reverts any changes that were
   * made. Note that if this method runs successfully, the AST will be modified with placeholder
   * types, and the caller should use the returned map (b in the pair) to revert those changes after
   * resolving the method call. If this method returns null, the AST will be unchanged.
   *
   * @param expr The expression to replace unresolvable type arguments in
   * @param fqnToCompilationUnits The map of FQNs to compilation units
   * @return A pair of a map of placeholder types to the original types/lambda return expr and a map
   *     of original nodes to their copies, or null if any unresolvable erased type is found
   */
  private static @Nullable Pair<Map<Type, Node>, IdentityHashMap<Node, Node>>
      replaceAllUnresolvableScopeTypeArgumentsWithResolvablePlaceholders(
          Expression expr, Map<String, CompilationUnit> fqnToCompilationUnits) {
    Map<Type, Node> placeholderToTypeHolding = new HashMap<>();
    IdentityHashMap<Node, Node> originalToCopiedNodes = new IdentityHashMap<>();
    Set<Expression> handled = new HashSet<>();

    if (expr.isMethodCallExpr()) {
      replaceUnsolvableLambdaBodiesInMethodCallArguments(
          expr.asMethodCallExpr(), originalToCopiedNodes, placeholderToTypeHolding);
    }

    boolean success =
        replaceAllUnresolvableScopeTypeArgumentsWithResolvablePlaceholdersImpl(
            expr, placeholderToTypeHolding, originalToCopiedNodes, handled, fqnToCompilationUnits);

    if (!success) {
      for (Map.Entry<Node, Node> entry : originalToCopiedNodes.entrySet()) {
        // Revert changes
        entry.getValue().replace(entry.getKey());
      }

      return null;
    }

    return new Pair<>(placeholderToTypeHolding, originalToCopiedNodes);
  }

  /**
   * Recursively replaces all unresolvable type arguments in the scope of a method call with
   * resolvable placeholder types, and stores the mapping from the placeholder types to the original
   * types in the result map. <br>
   * <br>
   * If any part of the scope has an erased type that cannot be resolved, this method returns false.
   * Note that changes may have been applied by this point, so the caller should check the result
   * map to see what changes were made and revert them if necessary.
   *
   * @param expr The expression to replace unresolvable type arguments in
   * @param placeholderToTypeHoldingNode A map of placeholder types to a "type-holding node", which
   *     is either an original Type or an Expression representing the lambda's return
   * @param originalToCopiedNodes A map of original nodes to their copies
   * @param handled The set of expressions that have already been handled
   * @param fqnToCompilationUnits The map of FQNs to compilation units
   * @return True if all unresolvable type arguments were successfully replaced, false if any
   */
  private static boolean replaceAllUnresolvableScopeTypeArgumentsWithResolvablePlaceholdersImpl(
      Expression expr,
      Map<Type, Node> placeholderToTypeHoldingNode,
      IdentityHashMap<Node, Node> originalToCopiedNodes,
      Set<Expression> handled,
      Map<String, CompilationUnit> fqnToCompilationUnits) {
    if (handled.contains(expr)) {
      return true;
    }
    handled.add(expr);

    // Get the innermost expression if there are parentheses (i.e., getting foo from ((((foo)))))
    while (expr.isEnclosedExpr()) {
      expr = expr.asEnclosedExpr().getInner();
    }

    // Start at the root of the scope expression: in this case, method() will be resolvable from foo
    // in foo.method()
    if (expr.hasScope()) {
      Expression scopeOfScope = ((NodeWithTraversableScope) expr).traverseScope().get();

      boolean success =
          replaceAllUnresolvableScopeTypeArgumentsWithResolvablePlaceholdersImpl(
              scopeOfScope,
              placeholderToTypeHoldingNode,
              originalToCopiedNodes,
              handled,
              fqnToCompilationUnits);
      if (!success) {
        return false;
      }
    }

    Type type = null;
    if (expr.isNameExpr() || expr.isFieldAccessExpr()) {
      ResolvedValueDeclaration resolved = null;

      if (expr.isNameExpr()) {
        resolved = Resolver.resolve(expr.asNameExpr());
      } else {
        resolved = Resolver.resolve(expr.asFieldAccessExpr());
      }

      if (resolved == null) {
        return false;
      }

      type = getTypeFromResolvedValueDeclaration(resolved, fqnToCompilationUnits);

      if (type == null) {
        // type may be null if the resolved field is from the JDK; in that case, return true because
        // all types in the JDK are solvable, so we don't need to replace any type arguments with
        // placeholders

        return resolved instanceof ReflectionFieldDeclaration;
      }
    } else if (expr.isMethodCallExpr()) {
      ResolvedMethodDeclaration resolvedScopeMethod = Resolver.resolve(expr.asMethodCallExpr());

      if (resolvedScopeMethod == null) {
        return false;
      }

      Node attachedNode = tryFindAttachedNode(resolvedScopeMethod, fqnToCompilationUnits);
      if (!(attachedNode instanceof MethodDeclaration methodDecl)) {
        // methodDecl may be null if the resolved method is from the JDK; in that case, return true
        // because all types in the JDK are solvable, so we don't need to replace any type arguments
        // with placeholders

        return resolvedScopeMethod instanceof ReflectionMethodDeclaration;
      }

      type = methodDecl.getType();

      replaceUnsolvableLambdaBodiesInMethodCallArguments(
          expr.asMethodCallExpr(), originalToCopiedNodes, placeholderToTypeHoldingNode);
    } else if (expr.isAssignExpr()) {
      // Assign expressions (x = y) return the assignment (y)
      return replaceAllUnresolvableScopeTypeArgumentsWithResolvablePlaceholdersImpl(
          expr.asAssignExpr().getValue(),
          placeholderToTypeHoldingNode,
          originalToCopiedNodes,
          handled,
          fqnToCompilationUnits);
    } else if (expr.isCastExpr()) {
      type = expr.asCastExpr().getType();
    } else if (expr.isConditionalExpr()) {
      // Do both sides of the conditional expression so the original expression can be resolved
      boolean success =
          replaceAllUnresolvableScopeTypeArgumentsWithResolvablePlaceholdersImpl(
              expr.asConditionalExpr().getThenExpr(),
              placeholderToTypeHoldingNode,
              originalToCopiedNodes,
              handled,
              fqnToCompilationUnits);
      if (!success) {
        return false;
      }
      success =
          replaceAllUnresolvableScopeTypeArgumentsWithResolvablePlaceholdersImpl(
              expr.asConditionalExpr().getElseExpr(),
              placeholderToTypeHoldingNode,
              originalToCopiedNodes,
              handled,
              fqnToCompilationUnits);
      if (!success) {
        return false;
      }
    } else if (expr.isObjectCreationExpr()) {
      type = expr.asObjectCreationExpr().getType();
    }

    if (type == null) {
      return false;
    }

    replaceInnermostUnresolvableTypeArgument(
        type, placeholderToTypeHoldingNode, originalToCopiedNodes);
    return true;
  }

  /**
   * Replaces unsolvable lambda bodies in method call arguments with resolvable placeholder types,
   * and stores the mapping from the placeholder types to the original lambda return expressions in
   * the result map. For example, in foo(x -> x.method()) where x.method() is unsolvable, this
   * method will replace the body of the lambda with a resolvable placeholder type, and store the
   * mapping from that placeholder type to the original lambda body expression (x.method()) in the
   * result map.
   *
   * @param methodCall The method call expression to replace unsolvable lambda bodies in
   * @param originalToCopiedNodes A map of original nodes to their copies, used to revert changes
   *     after resolving
   * @param placeholderToTypeHoldingNode A map of placeholder types to the original lambda return
   *     expressions
   */
  private static void replaceUnsolvableLambdaBodiesInMethodCallArguments(
      MethodCallExpr methodCall,
      Map<Node, Node> originalToCopiedNodes,
      Map<Type, Node> placeholderToTypeHoldingNode) {
    List<LambdaExpr> lambdas = methodCall.findAll(LambdaExpr.class).stream().toList();

    for (LambdaExpr lambda : lambdas) {
      // Try to replace unsolvable lambdas with known return types
      if (Resolver.calculateResolvedType(lambda) != null) {
        continue;
      }

      if (lambda.getBody().isBlockStmt()
          && lambda.getBody().asBlockStmt().findFirst(ReturnStmt.class).isEmpty()) {
        // If void, do not replace with a non-void return type
        continue;
      }

      LambdaExpr copy = lambda.clone();
      lambda.replace(copy);

      ClassOrInterfaceType placeholder =
          getResolvablePlaceholderType(placeholderToTypeHoldingNode.size());

      ObjectCreationExpr placeholderInstantiation = new ObjectCreationExpr();
      placeholderInstantiation.setType(placeholder);

      copy.setBody(new ExpressionStmt(placeholderInstantiation));

      // Find the "type-holding" node of the lambda expression (the expression stmt, or a return
      // stmt)

      if (lambda.getBody().isExpressionStmt()) {
        // If the lambda body is an expression statement, then the lambda itself is the
        // type-holding node
        placeholderToTypeHoldingNode.put(placeholder, lambda.getExpressionBody().get());
      } else {
        // If the lambda body is a block statement, then the return statement is the type-holding
        // node
        ReturnStmt returnStmt =
            lambda.getBody().asBlockStmt().findFirst(ReturnStmt.class).orElseThrow();
        placeholderToTypeHoldingNode.put(placeholder, returnStmt);
      }

      originalToCopiedNodes.put(lambda, copy);
    }
  }

  /**
   * Recursively replaces the innermost unresolvable type argument with a resolvable placeholder
   * type. For example, List<List<Foo>> should replace Foo with a placeholder, not List<Foo>.
   *
   * @param unresolvableType The unresolvable type to replace or to keep looking into
   * @param placeholderToReal A map of cloned types to their original types
   */
  private static void replaceInnermostUnresolvableTypeArgument(
      Type unresolvableType,
      Map<Type, Node> placeholderToReal,
      IdentityHashMap<Node, Node> originalToCopiedNodes) {
    if (unresolvableType.isClassOrInterfaceType()) {
      if (unresolvableType.asClassOrInterfaceType().getTypeArguments().isEmpty()
          || unresolvableType.asClassOrInterfaceType().getTypeArguments().get().isEmpty()) {
        // Use result.size() to ensure a 1-to-1 mapping between placeholder types and unresolvable
        // type arguments
        Type placeholder = getResolvablePlaceholderType(placeholderToReal.size());

        placeholderToReal.put(placeholder, unresolvableType);
        originalToCopiedNodes.put(unresolvableType, placeholder);
        unresolvableType.replace(placeholder);
        return;
      }

      ClassOrInterfaceType asClass = unresolvableType.asClassOrInterfaceType();
      NodeList<Type> typeArgs = asClass.getTypeArguments().get();

      for (Type typeArg : typeArgs) {
        if (Resolver.resolve(typeArg) != null) {
          continue;
        }
        replaceInnermostUnresolvableTypeArgument(typeArg, placeholderToReal, originalToCopiedNodes);
      }
    } else if (unresolvableType.isArrayType()) {
      replaceInnermostUnresolvableTypeArgument(
          unresolvableType.asArrayType().getComponentType(),
          placeholderToReal,
          originalToCopiedNodes);
    }
  }

  /**
   * Generates a resolvable placeholder type with the given index. Used as a placeholder for
   * unsolvable type arguments, so that we can still resolve a resolvable type usage that contains
   * these type arguments.
   *
   * @param index The index of the placeholder type, used to generate different placeholder types
   * @return A ClassOrInterfaceType that can be resolved to a dummy class
   */
  private static ClassOrInterfaceType getResolvablePlaceholderType(int index) {
    String typeName = "SPECIMIN_PLACEHOLDER_TYPE_" + index;

    if (generatedResolvedPlaceholderTypes.contains(typeName)) {
      return StaticJavaParser.parseClassOrInterfaceType(typeName);
    }

    // Register a dummy class with the type solver so that when we try to resolve this type, it
    // doesn't throw an exception
    // Add to java.lang so we don't need to worry about imports; this is a bit hacky but it works
    CompilationUnit dummy =
        StaticJavaParser.parse("package java.lang; public class " + typeName + " {}");
    getTypeSolvers().getMemoryTypeSolver().addType("java.lang." + typeName, dummy);

    generatedResolvedPlaceholderTypes.add(typeName);
    return StaticJavaParser.parseClassOrInterfaceType(typeName);
  }

  /**
   * Removes array brackets from a type name. i.e., int[][][] -> int
   *
   * @param name The name of the type
   * @return The name of the type, without the array brackets
   */
  public static String removeArrayBrackets(String name) {
    return name.replaceAll("(\\[])+$", "");
  }

  /**
   * Counts the number of array brackets in a type. If the type is int[][][], this method returns 3.
   *
   * @param name The name of the type
   * @return The number of array brackets in the type
   */
  public static int countNumberOfArrayBrackets(String name) {
    int count = 0;

    for (int i = name.length() - 1;
        i > 0 && name.charAt(i) == ']' && name.charAt(i - 1) == '[';
        i -= 2) {
      count++;
    }

    return count;
  }

  /**
   * When getting the scope/children of a ClassOrInterfaceType, it returns another
   * ClassOrInterfaceType. However, it does not differentiate between whether this type is a package
   * or if it's another type, so this method helps to differentiate between the two.
   *
   * @param type The type
   * @return True if the type is probably a package, based on conventions
   */
  public static boolean isProbablyAPackage(ClassOrInterfaceType type) {
    if (type.getTypeArguments().isPresent()) {
      return false;
    }

    return isProbablyAPackage(type.getNameAsString());
  }

  /**
   * Returns true if the given string is probably a package name. This is a heuristic based on
   * common Java package naming conventions. If the first character and each character after a dot
   * are all lowercase, then this is probably a package.
   *
   * @param type The type/package name
   * @return True if the type is probably a package
   */
  public static boolean isProbablyAPackage(String type) {
    for (String segment : type.split("\\.", -1)) {
      if (Character.isUpperCase(segment.charAt(0))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks to see if an expression (FieldAccessExpr or NameExpr) is likely part of a package. This
   * checks parents too, so org.example would be seen as org.example.Test, allowing us to
   * differentiate between part of a package and a field name.
   *
   * @param type The type
   * @return True if the type is probably a package, based on conventions
   */
  public static boolean isProbablyAPackage(Expression type) {
    if (!type.isFieldAccessExpr() && !type.isNameExpr()) {
      return false;
    }

    // Baz in Baz.myField
    if (type.isNameExpr() && isAClassName(type.toString())) {
      return false;
    }

    if (type.isFieldAccessExpr() && isAClassPath(type.toString())) {
      return false;
    }

    while (type.hasParentNode() && type.getParentNode().get() instanceof FieldAccessExpr field) {
      type = field;
      if (isAClassPath(type.toString())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Tries to get the method definition if the method call expression matches that of a method
   * included in the JDK. Returns an empty list if it cannot be resolved.
   *
   * @param methodCall The method call expression to resolve
   * @return A list of matching method definitions, or an empty list if none are found
   */
  public static List<MethodUsage> tryGetJDKDefsWithUnresolvableArguments(
      MethodCallExpr methodCall) {
    ResolvedReferenceTypeDeclaration typeDecl;

    if (methodCall.hasScope()) {
      Expression scope = methodCall.getScope().get();
      ResolvedType scopeType = Resolver.calculateResolvedType(scope);

      if (scopeType == null) {
        return Collections.emptyList();
      }

      if (scopeType.isReferenceType()) {
        typeDecl = scopeType.asReferenceType().getTypeDeclaration().get();
      } else {
        return Collections.emptyList();
      }
    } else {
      return Collections.emptyList();
    }

    int numArgs = methodCall.getArguments().size();
    String name = methodCall.getNameAsString();

    return typeDecl.getAllMethods().stream()
        .filter(m -> m.getName().equals(name) && m.getParamTypes().size() == numArgs)
        .collect(Collectors.toList());
  }

  /**
   * Tries to get the constructor definition if the constructor call expression matches that of a
   * constructor included in the JDK. Returns an empty list if it cannot be resolved. For method
   * call expressions, use {@link #tryGetJDKDefsWithUnresolvableArguments(MethodCallExpr)} instead.
   *
   * @param withArgs The node with arguments to resolve (a constructor call or enum constant
   *     declaration)
   * @return A list of matching constructor definitions, or an empty list if none are found
   */
  public static List<ResolvedConstructorDeclaration> tryGetJDKDefsWithUnresolvableArguments(
      NodeWithArguments<?> withArgs) {
    ResolvedReferenceTypeDeclaration typeDecl;

    if (withArgs instanceof NodeWithTraversableScope withScope
        && withScope.traverseScope().isPresent()) {
      Expression scope = withScope.traverseScope().get();
      ResolvedType scopeType = Resolver.calculateResolvedType(scope);

      if (scopeType == null) {
        return Collections.emptyList();
      }

      if (scopeType.isReferenceType()) {
        typeDecl = scopeType.asReferenceType().getTypeDeclaration().get();
      } else {
        return Collections.emptyList();
      }
    } else {
      return Collections.emptyList();
    }

    int numArgs = withArgs.getArguments().size();
    if (withArgs instanceof MethodCallExpr) {
      throw new RuntimeException(
          "Call tryGetJDKDefsWithUnresolvableArguments(MethodCallExpr, Map) instead");
    } else {
      return typeDecl.getConstructors().stream()
          .filter(c -> c.getNumberOfParams() == numArgs)
          .collect(Collectors.toList());
    }
  }

  /**
   * Returns a list of existing callable declarations (methods, constructors, or enum constants)
   * that match the given node with arguments, even if some arguments are unresolvable.
   *
   * @param withArgs The node representing a method call, constructor call, or enum constant
   *     declaration with arguments
   * @param fqnsToCompilationUnits The map of fully qualified names to their compilation units
   * @return A list of matching {@link CallableDeclaration} instances, or an empty list if none are
   *     found
   */
  public static List<? extends CallableDeclaration<?>> tryResolveNodeWithUnresolvableArguments(
      NodeWithArguments<?> withArgs, Map<String, CompilationUnit> fqnsToCompilationUnits) {
    if (withArgs instanceof MethodCallExpr parentMethodCall) {
      return tryResolveMethodCallWithUnresolvableArguments(
          parentMethodCall, fqnsToCompilationUnits);
    } else if (withArgs instanceof ExplicitConstructorInvocationStmt parentConstructorCall) {
      return tryResolveConstructorCallWithUnresolvableArguments(
          parentConstructorCall, fqnsToCompilationUnits);
    } else if (withArgs instanceof ObjectCreationExpr parentConstructorCall) {
      return tryResolveConstructorCallWithUnresolvableArguments(
          parentConstructorCall, fqnsToCompilationUnits);
    } else if (withArgs instanceof EnumConstantDeclaration parentEnumConstantDeclaration) {
      return tryResolveEnumConstantDeclarationWithUnresolvableArguments(
          parentEnumConstantDeclaration, fqnsToCompilationUnits);
    } else {
      // Not possible:
      // https://javadoc.io/doc/com.github.javaparser/javaparser-core/latest/com/github/javaparser/ast/nodeTypes/NodeWithArguments.html
      throw new RuntimeException("Unexpected NodeWithArguments type: " + withArgs.getClass());
    }
  }

  /**
   * Tries to resolve a node with unresolvable arguments. If no single callable can be found, it
   * returns null.
   *
   * @param node The node with arguments to resolve
   * @param fqnsToCompilationUnits The map of fully qualified names to their compilation units
   * @return A resolvable callable if it can be found, null otherwise
   */
  public static @Nullable CallableDeclaration<?>
      tryFindSingleCallableForNodeWithUnresolvableArguments(
          NodeWithArguments<?> node, Map<String, CompilationUnit> fqnsToCompilationUnits) {
    List<? extends CallableDeclaration<?>> callables =
        tryResolveNodeWithUnresolvableArguments(node, fqnsToCompilationUnits);
    if (callables.isEmpty()) {
      return null;
    }

    if (callables.size() == 1) {
      return callables.get(0);
    }

    List<@Nullable Object> argumentTypes =
        new ArrayList<>(getArgumentTypesAsResolved(node.getArguments()));

    for (int i = 0; i < argumentTypes.size(); i++) {
      if (argumentTypes.get(i) == null) {
        argumentTypes.set(
            i, tryGetTypeAsStringFromExpression(node.getArgument(i), fqnsToCompilationUnits));
      }
    }

    // If there is only one callable where the rest of the parameters match and a few others that
    // are null, return this maybe best match
    CallableDeclaration<?> maybeBestMatch = null;
    // If there are multiple callables, find the best match, i.e., FQNs = FQNs and simple names =
    // simple names
    // If there is one that matches exactly, return it; others that match it directly are simply
    // overrides
    for (CallableDeclaration<?> callable : callables) {
      boolean isAMatch = true;
      int nulls = 0;
      for (int i = 0; i < callable.getParameters().size(); i++) {
        Object typeInCall = argumentTypes.get(i);

        if (typeInCall == null) {
          nulls++;
          continue;
        }

        // The call to tryResolve... already guarantees that ResolvedType is handled correctly
        if (typeInCall instanceof ResolvedType) {
          continue;
        }

        Type paramType = callable.getParameter(i).getType();
        if (typeInCall instanceof String typeAsString) {
          // If the type in the call is a string, it must match the parameter type exactly
          if (!erase(paramType.asString()).equals(erase(typeAsString))) {
            isAMatch = false;
            break;
          }
        }
      }

      if (isAMatch) {
        if (nulls == 0) {
          // If there are no nulls, this is a perfect match
          return callable;
        } else if (maybeBestMatch == null) {
          // If there are nulls, this is the best match so far
          maybeBestMatch = callable;
        } else {
          // If nulls > 0 and maybeBestMatch is already existing, return null since we have
          // ambiguities: handle in UnsolvedSymbolGenerator
          return null;
        }
      }
    }

    return maybeBestMatch;
  }

  /**
   * Given a constructor call, returns all possible constructors that match the arity and known
   * types of the arguments. This returns an empty list if no matching constructors were found or if
   * the declaring type could not be solved.
   *
   * @param constructorCall The constructor call expression
   * @param fqnToCompilationUnits The map of type FQNs to their compilation units
   * @return All possible constructor declarations
   */
  private static List<ConstructorDeclaration> tryResolveConstructorCallWithUnresolvableArguments(
      ObjectCreationExpr constructorCall, Map<String, CompilationUnit> fqnToCompilationUnits) {
    List<@Nullable ResolvedType> parameterTypes =
        getArgumentTypesAsResolved(constructorCall.getArguments());

    TypeDeclaration<?> enclosingClass;

    ResolvedType type = Resolver.resolve(constructorCall.getType());

    if (type == null) {
      return List.of();
    }

    enclosingClass = getTypeFromQualifiedName(type.describe(), fqnToCompilationUnits);

    if (enclosingClass == null) {
      return List.of();
    }

    List<ConstructorDeclaration> candidates = new ArrayList<>();

    addAllMatchingCallablesToList(
        enclosingClass, parameterTypes, candidates, null, ConstructorDeclaration.class);

    return candidates;
  }

  /**
   * Given a constructor call, returns all possible constructors that match the arity and known
   * types of the arguments. This returns an empty list if no matching constructors were found or if
   * the declaring type could not be solved.
   *
   * @param constructorCall The constructor call statement (super or this constructor call)
   * @param fqnToCompilationUnits The map of type FQNs to their compilation units
   * @return All possible constructor declarations
   */
  private static List<ConstructorDeclaration> tryResolveConstructorCallWithUnresolvableArguments(
      ExplicitConstructorInvocationStmt constructorCall,
      Map<String, CompilationUnit> fqnToCompilationUnits) {
    List<@Nullable ResolvedType> parameterTypes =
        getArgumentTypesAsResolved(constructorCall.getArguments());

    TypeDeclaration<?> enclosingClass = getEnclosingClassLike(constructorCall);
    List<ConstructorDeclaration> candidates = new ArrayList<>();

    if (constructorCall.isThis()) {
      addAllMatchingCallablesToList(
          enclosingClass, parameterTypes, candidates, null, ConstructorDeclaration.class);
    } else {
      TypeDeclaration<?> parent = null;

      ResolvedType resolvedSuperClass = Resolver.resolve(getSuperClass(constructorCall));

      if (resolvedSuperClass != null) {
        parent = getTypeFromQualifiedName(resolvedSuperClass.describe(), fqnToCompilationUnits);
      }

      if (parent != null) {
        addAllMatchingCallablesToList(
            parent, parameterTypes, candidates, null, ConstructorDeclaration.class);
      }
    }

    return candidates;
  }

  /**
   * Given a method call, returns all possible methods that match the arity and known types of the
   * arguments. This returns an empty list if no matching methods were found or if the declaring
   * type could not be solved.
   *
   * @param methodCall The method call expression
   * @param fqnToCompilationUnits The map of type FQNs to their compilation units
   * @return All possible method declarations
   */
  private static List<MethodDeclaration> tryResolveMethodCallWithUnresolvableArguments(
      MethodCallExpr methodCall, Map<String, CompilationUnit> fqnToCompilationUnits) {
    boolean isSuperOnly = false;

    ObjectCreationExpr enclosingAnonymousClass = getEnclosingAnonymousClassIfExists(methodCall);

    List<TypeDeclaration<?>> enclosingClass = new ArrayList<>();

    if (enclosingAnonymousClass != null) {
      ResolvedType resolvedAnonClassType = Resolver.resolve(enclosingAnonymousClass.getType());
      if (resolvedAnonClassType != null) {
        TypeDeclaration<?> enclosingAnonymousClassType =
            getTypeFromQualifiedName(resolvedAnonClassType.describe(), fqnToCompilationUnits);

        if (enclosingAnonymousClassType != null) {
          enclosingClass.add(enclosingAnonymousClassType);
        }
      }
    }

    if (methodCall.hasScope()) {
      Expression scope = methodCall.getScope().get();

      if (scope.isSuperExpr()) {
        isSuperOnly = true;
      }

      ResolvedType scopeType = Resolver.calculateResolvedType(scope);

      if (scopeType != null) {
        if (scopeType.isTypeVariable()) {
          try {
            for (Bound bound : scopeType.asTypeParameter().getBounds()) {
              TypeDeclaration<?> decl =
                  getTypeFromQualifiedName(bound.getType().describe(), fqnToCompilationUnits);

              if (decl != null) {
                enclosingClass.add(decl);
              }
            }
          } catch (UnsolvedSymbolException ex) {
            // getBounds() can throw an UnsolvedSymbolException
          }

          if (enclosingClass.isEmpty()) {
            return List.of();
          }
        } else {
          TypeDeclaration<?> decl =
              getTypeFromQualifiedName(scopeType.describe(), fqnToCompilationUnits);

          if (decl == null) {
            return List.of();
          }

          enclosingClass.add(decl);
        }
      } else {
        // Maybe the scope has type arguments; try to resolve without those.
        String scopeTypeFQN =
            getQualifiedNameOfTypeOfExpressionWithUnresolvableTypeArgs(
                scope, fqnToCompilationUnits);

        if (scopeTypeFQN == null) {
          return List.of();
        }

        TypeDeclaration<?> decl = getTypeFromQualifiedName(scopeTypeFQN, fqnToCompilationUnits);

        if (decl == null) {
          return List.of();
        }

        enclosingClass.add(decl);
      }
    } else {
      enclosingClass.add(getEnclosingClassLike(methodCall));
    }

    List<@Nullable ResolvedType> parameterTypes =
        getArgumentTypesAsResolved(methodCall.getArguments());

    List<MethodDeclaration> candidates = new ArrayList<>();
    for (TypeDeclaration<?> typeDecl : enclosingClass) {
      if (!isSuperOnly) {
        addAllMatchingCallablesToList(
            typeDecl,
            parameterTypes,
            candidates,
            methodCall.getNameAsString(),
            MethodDeclaration.class);
      }

      for (TypeDeclaration<?> ancestor : getAllSolvableAncestors(typeDecl, fqnToCompilationUnits)) {
        addAllMatchingCallablesToList(
            ancestor,
            parameterTypes,
            candidates,
            methodCall.getNameAsString(),
            MethodDeclaration.class);
      }
    }

    if (enclosingAnonymousClass != null) {
      addAllMatchingCallablesToListImpl(
          enclosingAnonymousClass.getAnonymousClassBody().get().stream()
              .filter(BodyDeclaration::isCallableDeclaration)
              .map(c -> (CallableDeclaration<?>) c)
              .toList(),
          parameterTypes,
          candidates,
          methodCall.getNameAsString(),
          MethodDeclaration.class);
    }

    return candidates;
  }

  /**
   * Given an enum constant declaration, returns all possible constructors that match the arity and
   * known types of the arguments. This returns an empty list if no matching constructors were found
   * or if the declaring type could not be solved.
   *
   * @param enumConstant The enum constant declaration
   * @param fqnToCompilationUnits The map of type FQNs to their compilation units
   * @return All possible constructor declarations
   */
  private static List<ConstructorDeclaration>
      tryResolveEnumConstantDeclarationWithUnresolvableArguments(
          EnumConstantDeclaration enumConstant,
          Map<String, CompilationUnit> fqnToCompilationUnits) {
    List<@Nullable ResolvedType> parameterTypes =
        getArgumentTypesAsResolved(enumConstant.getArguments());

    TypeDeclaration<?> enclosingClass;

    ResolvedEnumConstantDeclaration resolvedDecl = Resolver.resolve(enumConstant);

    if (resolvedDecl == null) {
      return List.of();
    }
    ResolvedType type = resolvedDecl.getType();

    enclosingClass = getTypeFromQualifiedName(type.describe(), fqnToCompilationUnits);

    if (enclosingClass == null) {
      return List.of();
    }

    List<ConstructorDeclaration> candidates = new ArrayList<>();

    addAllMatchingCallablesToList(
        enclosingClass, parameterTypes, candidates, null, ConstructorDeclaration.class);

    return candidates;
  }

  /**
   * Given an expression that has a resolvable definition, return the FQN of its resolved type if
   * .calculateResolvedType() fails because of unresolvable type arguments. Returns the string
   * instead of the ResolvedType to prevent unintentional use of the resulting ResolvedType, which
   * uses a detached node.
   *
   * @param expr The expression whose type needs to be resolved
   * @param fqnToCompilationUnits The map of FQNs to compilation units
   * @return The FQN of the resolved type if found, null otherwise
   */
  public static @Nullable String getQualifiedNameOfTypeOfExpressionWithUnresolvableTypeArgs(
      Expression expr, Map<String, CompilationUnit> fqnToCompilationUnits) {
    if (!(expr instanceof Resolvable<?> resolvable)) {
      return null;
    }
    Object resolved = Resolver.resolve(resolvable);

    if (resolved == null) {
      return null;
    }

    ClassOrInterfaceType classOrInterfaceType = null;
    if (resolved instanceof ResolvedValueDeclaration resolvedValueDecl) {
      Type type = getTypeFromResolvedValueDeclaration(resolvedValueDecl, fqnToCompilationUnits);

      if (type != null && type.isClassOrInterfaceType()) {
        classOrInterfaceType = type.asClassOrInterfaceType();
      }
    } else if (resolved instanceof ResolvedMethodDeclaration resolvedMethodDeclaration) {
      MethodDeclaration method =
          (MethodDeclaration) tryFindAttachedNode(resolvedMethodDeclaration, fqnToCompilationUnits);

      if (method != null) {
        Type type = method.getType();
        if (type != null && type.isClassOrInterfaceType()) {
          classOrInterfaceType = type.asClassOrInterfaceType();
        }
      }
    }

    if (classOrInterfaceType == null) {
      return null;
    }

    // I tried cloning the type and temporarily adding it to the compilation unit, but doing so
    // prevents it from being removed, which did change the output of some test cases. We'll
    // do this instead, but it's definitely not pretty.
    Optional<NodeList<Type>> typeArgs = classOrInterfaceType.getTypeArguments();
    classOrInterfaceType.removeTypeArguments();

    ResolvedType resolvedType = Resolver.resolve(classOrInterfaceType);

    if (typeArgs.isPresent()) {
      classOrInterfaceType.setTypeArguments(typeArgs.get());
    }

    if (resolvedType == null) {
      return null;
    }

    return resolvedType.describe();
  }

  /**
   * Helper method for {@link #tryResolveConstructorCallWithUnresolvableArguments}. Gets argument
   * types as their resolved counterparts and null if unresolvable.
   *
   * @param arguments The arguments, as a list of expressions
   * @return The list of resolved types, or null if unresolvable
   */
  public static List<@Nullable ResolvedType> getArgumentTypesAsResolved(
      List<Expression> arguments) {
    return arguments.stream().map(Resolver::calculateResolvedType).collect(Collectors.toList());
  }

  /**
   * Helper method for {@link #tryResolveConstructorCallWithUnresolvableArguments} and {@link
   * #tryResolveMethodCallWithUnresolvableArguments}. Adds all callables (constructors/methods) that
   * match the given parameterTypes to the output list.
   *
   * @param typeDecl The type declaration to search through
   * @param parameterTypes The resolved parameter types. Fully qualified names if resolvable, simple
   *     names if not, and null if no type could be found at all.
   * @param result The list to append to
   * @param methodName The method name, if the callable is a method (it is ignored otherwise)
   * @param callableType The type of callable (i.e., ConstructorDeclaration or MethodDeclaration)
   */
  private static <T extends CallableDeclaration<?>> void addAllMatchingCallablesToList(
      TypeDeclaration<?> typeDecl,
      List<@Nullable ResolvedType> parameterTypes,
      List<T> result,
      @Nullable String methodName,
      Class<T> callableType) {
    List<? extends CallableDeclaration<?>> callables;
    if (callableType == ConstructorDeclaration.class) {
      callables = typeDecl.getConstructors();
    } else if (callableType == MethodDeclaration.class) {
      callables = typeDecl.getMethods();
    } else {
      // Impossible: see
      // https://www.javadoc.io/doc/com.github.javaparser/javaparser-core/latest/com/github/javaparser/ast/body/CallableDeclaration.html
      throw new IllegalArgumentException("Impossible CallableDeclaration type.");
    }

    addAllMatchingCallablesToListImpl(callables, parameterTypes, result, methodName, callableType);
  }

  /**
   * Actual logic for {@link #addAllMatchingCallablesToList}.
   *
   * @param callables The list of callables to check against
   * @param parameterTypes The resolved parameter types. Fully qualified names if resolvable, simple
   *     names if not, and null if no type could be found at all.
   * @param result The list to append to
   * @param methodName The method name, if the callable is a method (it is ignored otherwise)
   * @param callableType The type of callable (i.e., ConstructorDeclaration or MethodDeclaration)
   */
  private static <T extends CallableDeclaration<?>> void addAllMatchingCallablesToListImpl(
      List<? extends CallableDeclaration<?>> callables,
      List<@Nullable ResolvedType> parameterTypes,
      List<T> result,
      @Nullable String methodName,
      Class<T> callableType) {
    for (CallableDeclaration<?> callable : callables) {
      if (callable.getParameters().size() != parameterTypes.size()) {
        continue;
      }
      if (callableType == MethodDeclaration.class
          && !callable.getNameAsString().equals(methodName)) {
        continue;
      }

      boolean isAMatch = true;

      for (int i = 0; i < callable.getParameters().size(); i++) {
        ResolvedType typeInCall = parameterTypes.get(i);

        ResolvedParameterDeclaration resolvedParam = Resolver.resolve(callable.getParameter(i));
        boolean isParamTypeUnsolved = resolvedParam == null;

        if (resolvedParam != null) {
          ResolvedType resolvedParameterType = null;
          try {
            resolvedParameterType = resolvedParam.getType();
          } catch (UnsolvedSymbolException ex) {
            isParamTypeUnsolved = true;
            // getType() may throw an UnsolvedSymbolException
          }

          if (typeInCall == null || isParamTypeUnsolved || resolvedParameterType == null) {
            continue;
          }

          if (!resolvedParameterType.isAssignableBy(typeInCall)) {
            // If either is a type variable and the other is a reference type, it is likely valid
            // Note that isAssignableBy will return false in those cases
            if (typeInCall.isTypeVariable() && resolvedParameterType.isReference()) {
              continue;
            }
            if (resolvedParameterType.isTypeVariable() && typeInCall.isReference()) {
              continue;
            }
            // JavaParser can't handle constraint types well. This isn't perfect (i.e., doesn't
            // properly match bounds), but it should work for most cases.
            if (typeInCall.isConstraint() && resolvedParameterType.isReference()) {
              continue;
            }
            isAMatch = false;
            break;
          }
        }

        if (isParamTypeUnsolved && typeInCall != null) {
          isAMatch = false;
          break;
        }
      }

      if (isAMatch) {
        result.add(callableType.cast(callable));
      }
    }
  }

  /**
   * Utility method to get the extended/implemented types from a type declaration.
   *
   * @param typeDecl The type declaration
   * @return A list of direct super types (extended/implemented)
   */
  public static List<ClassOrInterfaceType> getDirectSuperTypes(TypeDeclaration<?> typeDecl) {
    List<ClassOrInterfaceType> extendedOrImplemented = new ArrayList<>();

    if (typeDecl instanceof NodeWithExtends<?> withExtends) {
      extendedOrImplemented.addAll(withExtends.getExtendedTypes());
    }
    if (typeDecl instanceof NodeWithImplements<?> withImplements) {
      extendedOrImplemented.addAll(withImplements.getImplementedTypes());
    }

    return extendedOrImplemented;
  }

  /**
   * Finds all unsolvable ancestors, given a type declaration to start.
   *
   * @param start The type declaration
   * @param fqnToCompilationUnits A map of FQNs to compilation units
   * @return A list of type declarations representing all unsolvable ancestors
   */
  public static List<ClassOrInterfaceType> getAllUnsolvableAncestors(
      TypeDeclaration<?> start, Map<String, CompilationUnit> fqnToCompilationUnits) {
    List<ClassOrInterfaceType> result = new ArrayList<>();

    getAllUnsolvableAncestorsImpl(start, fqnToCompilationUnits, result);

    return result;
  }

  /**
   * Helper method for {@link #getAllUnsolvableAncestors(TypeDeclaration, Map)}. Recursively calls
   * itself on all unsolvable ancestors.
   *
   * @param start The declaration to start at
   * @param fqnToCompilationUnits The map of FQNs to compilation units
   * @param result The
   */
  private static void getAllUnsolvableAncestorsImpl(
      TypeDeclaration<?> start,
      Map<String, CompilationUnit> fqnToCompilationUnits,
      List<ClassOrInterfaceType> result) {
    List<ClassOrInterfaceType> extendedOrImplemented = getDirectSuperTypes(start);

    for (ClassOrInterfaceType type : extendedOrImplemented) {
      ResolvedType resolvedType = Resolver.resolve(type);

      if (resolvedType == null) {
        result.add(type);
        continue;
      }

      TypeDeclaration<?> typeDecl =
          getTypeFromQualifiedName(resolvedType.describe(), fqnToCompilationUnits);

      if (typeDecl == null) {
        continue;
      }

      getAllUnsolvableAncestorsImpl(typeDecl, fqnToCompilationUnits, result);
    }
  }

  /**
   * Finds all solvable ancestors (in JDK and user-defined types), given a type declaration to
   * start.
   *
   * @param start The type declaration
   * @return A set of resolved type declarations representing all solvable ancestors
   */
  public static Set<ResolvedReferenceTypeDeclaration> getAllJDKAncestors(TypeDeclaration<?> start) {
    Set<ResolvedReferenceTypeDeclaration> result = new HashSet<>();

    // TypeDeclaration<?> doesn't implement Resolvable<ResolvedReferenceTypeDeclaration>, but
    // contains
    // an abstract method resolve() returning type ResolvedReferenceTypeDeclaration
    ResolvedReferenceTypeDeclaration resolved =
        (ResolvedReferenceTypeDeclaration) Resolver.resolve((Resolvable<?>) start);
    if (resolved != null) {
      getAllJDKAncestorsImpl(resolved, result);
    }

    return result;
  }

  /**
   * Finds all JDK ancestors of a given type recursively.
   *
   * @param type The type declaration
   * @param result The set to append to
   */
  private static void getAllJDKAncestorsImpl(
      ResolvedReferenceTypeDeclaration type, Set<ResolvedReferenceTypeDeclaration> result) {
    for (ResolvedReferenceType ancestor : type.getAncestors(true)) {
      if (JavaLangUtils.inJdkPackage(ancestor.getQualifiedName())) {
        result.add(ancestor.getTypeDeclaration().get());
      }

      getAllJDKAncestorsImpl(ancestor.getTypeDeclaration().get(), result);
    }
  }

  /**
   * Finds all solvable non-JDK ancestors, given a type declaration to start.
   *
   * @param start The type declaration
   * @param fqnToCompilationUnits A map of FQNs to compilation units
   * @return A list of type declarations representing all solvable ancestors
   */
  public static List<TypeDeclaration<?>> getAllSolvableAncestors(
      TypeDeclaration<?> start, Map<String, CompilationUnit> fqnToCompilationUnits) {
    List<TypeDeclaration<?>> result = new ArrayList<>();

    getAllSolvableAncestorsImpl(start, fqnToCompilationUnits, result);

    return result;
  }

  /**
   * Helper method for {@link #getAllSolvableAncestors(TypeDeclaration, Map)}. Recursively calls
   * itself on all solvable ancestors.
   *
   * @param start The declaration to start at
   * @param fqnToCompilationUnits The map of FQNs to compilation units
   * @param result The
   */
  private static void getAllSolvableAncestorsImpl(
      TypeDeclaration<?> start,
      Map<String, CompilationUnit> fqnToCompilationUnits,
      List<TypeDeclaration<?>> result) {
    List<ClassOrInterfaceType> extendedOrImplemented = getDirectSuperTypes(start);

    for (ClassOrInterfaceType type : extendedOrImplemented) {
      ResolvedType resolvedType = Resolver.resolve(type);

      if (resolvedType == null) {
        continue;
      }

      TypeDeclaration<?> typeDecl =
          getTypeFromQualifiedName(resolvedType.describe(), fqnToCompilationUnits);

      if (typeDecl == null) {
        continue;
      }

      result.add(typeDecl);
      getAllSolvableAncestorsImpl(typeDecl, fqnToCompilationUnits, result);
    }
  }

  /**
   * Gets the corresponding type declaration from a qualified type name. Use this method instead of
   * casting from toAst() to avoid resolve() errors on child nodes.
   *
   * @param fqn The fully-qualified type name; no need to erase because this method does it.
   * @param fqnToCompilationUnits A map of fully-qualified type names to their compilation units
   * @return The type declaration; null if not in the project.
   */
  public static @Nullable TypeDeclaration<?> getTypeFromQualifiedName(
      String fqn, Map<String, CompilationUnit> fqnToCompilationUnits) {

    String erased = erase(fqn);
    String searchFQN = erased;

    CompilationUnit someCandidate = fqnToCompilationUnits.get(searchFQN);
    while (searchFQN.contains(".") && someCandidate == null) {
      searchFQN = searchFQN.substring(0, searchFQN.lastIndexOf('.'));
      someCandidate = fqnToCompilationUnits.get(searchFQN);
    }

    if (someCandidate == null) {
      // Not in project; solved by reflection, not our concern
      return null;
    }

    return (TypeDeclaration<?>)
        someCandidate
            .findFirst(
                TypeDeclaration.class,
                n ->
                    n.getFullyQualifiedName().isPresent()
                        && n.getFullyQualifiedName().get().equals(erased))
            .orElse(null);
  }

  /**
   * Given an AssociableToAST that could give a detached node, find its attached equivalent. This
   * method is only necessary when you need to call resolve() or calculateResolvedType() on its
   * children. Throws if the result is null; use {@link #tryFindAttachedNode(AssociableToAST, Map)}
   * if you do not want this.
   *
   * @param associable The resolved definition that could yield a detached node
   * @param fqnToCompilationUnits A map of fully-qualified type names to their compilation units
   * @return The attached node
   */
  public static Node findAttachedNode(
      AssociableToAST associable, Map<String, CompilationUnit> fqnToCompilationUnits) {
    Node result = tryFindAttachedNode(associable, fqnToCompilationUnits);

    if (result == null) {
      throw new RuntimeException("Could not find an attached AST node.");
    }

    return result;
  }

  /**
   * Given an AssociableToAST that could give a detached node, find its attached equivalent. This
   * method is only necessary when you need to call resolve() or calculateResolvedType() on its
   * children.
   *
   * @param associable The resolved definition that could yield a detached node
   * @param fqnToCompilationUnits A map of fully-qualified type names to their compilation units
   * @return The attached node if found, or null if not found
   */
  public static @Nullable Node tryFindAttachedNode(
      AssociableToAST associable, Map<String, CompilationUnit> fqnToCompilationUnits) {
    Node detachedNode = associable.toAst().orElse(null);

    if (detachedNode == null) {
      return null;
    }

    TypeDeclaration<?> declaration = getEnclosingClassLike(detachedNode);

    TypeDeclaration<?> attached =
        getTypeFromQualifiedName(
            declaration.getFullyQualifiedName().orElse(""), fqnToCompilationUnits);

    if (attached == null) {
      return null;
    }

    Class<? extends Node> nodeClass = detachedNode.getClass();
    return attached.findFirst(nodeClass, n -> n.equals(detachedNode)).orElse(null);
  }

  /**
   * Returns a type-compatible initializer for a field of the given type.
   *
   * @param variableType the type of the field
   * @return a type-compatible initializer
   */
  public static String getInitializerRHS(String variableType) {
    return switch (variableType) {
      case "byte" -> "(byte)0";
      case "short" -> "(short)0";
      case "int" -> "0";
      case "long" -> "0L";
      case "float" -> "0.0f";
      case "double" -> "0.0d";
      case "char" -> "'\\u0000'";
      case "boolean" -> "false";
      default -> "null";
    };
  }

  /**
   * Given a list of parameter types, return a super(...) call with the default values of those
   * types.
   *
   * @param paramTypes The parameter types
   * @return The super(...) call; i.e., super(0, null);
   */
  public static String getDefaultSuperConstructorCall(List<String> paramTypes) {
    StringBuilder sb = new StringBuilder();

    sb.append("super(");

    for (String paramType : paramTypes) {
      sb.append(getInitializerRHS(paramType));
      sb.append(", ");
    }

    // remove the trailing ", "
    sb.delete(sb.length() - 2, sb.length());

    sb.append(");");
    return sb.toString();
  }

  /**
   * Get the enclosing anonymous class, if node is in one. If not, then this method returns null.
   *
   * @param node The node
   * @return The enclosing anonymous class, or null if not found
   */
  public static @Nullable ObjectCreationExpr getEnclosingAnonymousClassIfExists(Node node) {
    Node parent = node.getParentNode().orElse(null);
    Set<Node> parents = new HashSet<>();

    while (parent != null) {
      if (parent instanceof ObjectCreationExpr objectCreationExpr) {
        if (objectCreationExpr.getAnonymousClassBody().isEmpty()) {
          return null;
        }

        Class<? extends Node> nodeClass = node.getClass();
        if (objectCreationExpr.getType().findFirst(nodeClass, n -> n.equals(node)).isPresent()) {
          return objectCreationExpr;
        }

        for (BodyDeclaration<?> anonymousBodyDecl :
            objectCreationExpr.getAnonymousClassBody().get()) {
          if (parents.contains(anonymousBodyDecl)) {
            return objectCreationExpr;
          }
        }

        return null;
      }
      parents.add(parent);
      parent = parent.getParentNode().orElse(null);
    }

    return null;
  }

  /**
   * Given a resolvable node that may be in an anonymous class, try to resolve it. If it is not in
   * an anonymous class or cannot be resolved, return null.
   *
   * @param nodeToResolve The node to resolve
   * @return The resolved value, or null if not found
   */
  public static @Nullable Object tryResolveNodeIfInAnonymousClass(Node nodeToResolve) {
    ObjectCreationExpr anonymousClassDecl = getEnclosingAnonymousClassIfExists(nodeToResolve);

    if (anonymousClassDecl == null) {
      return null;
    }

    // If in a callable declaration, add a temporary statement above the current
    Statement current = null;

    Node node = anonymousClassDecl.getParentNode().orElse(null);
    while (node != null) {
      if (node instanceof Statement statement) {
        current = statement;
        break;
      }
      node = node.getParentNode().orElse(null);
    }

    if (current == null) {
      return null;
    }

    Node copy = nodeToResolve.clone();

    try {
      // Temporarily insert a copy of the node outside the anonymous class and
      // see if it is resolvable
      copy.setParentNode(current);

      // Not all nodes are resolvable (some expressions aren't)
      if (copy instanceof Resolvable<?> resolvable) {
        Object resolved = Resolver.resolve(resolvable);

        if (resolved != null) {
          return resolved;
        }
      }

      if (copy instanceof Expression expression) {
        return Resolver.calculateResolvedType(expression);
      }

      return null;
    } finally {
      copy.remove();
    }
  }

  /**
   * Tries to find the corresponding declaration in an anonymous class. For example, a NameExpr
   * would return its FieldDeclaration. This method is necessary since resolve() fails in an
   * anonymous class with an unsolvable parent class, even if the member is defined within the
   * anonymous class. Returns null if not found, or if the expression is not in an anonymous class.
   *
   * @param expr The expression
   * @return The corresponding declaration, or null if not found
   */
  public static @Nullable BodyDeclaration<?> tryFindCorrespondingDeclarationInAnonymousClass(
      Expression expr) {
    ObjectCreationExpr anonymousClass = getEnclosingAnonymousClassIfExists(expr);

    if (anonymousClass == null) {
      return null;
    }

    // Check if the expression is within the anonymous class. This method is necessary because
    // if the parent class of the anonymous class is not solvable, fields/methods defined within
    // are also unsolvable
    if (expr.isNameExpr()
        || (expr.isFieldAccessExpr() && expr.asFieldAccessExpr().getScope().isThisExpr())) {
      // Try to find the field
      for (BodyDeclaration<?> bodyDecl : anonymousClass.getAnonymousClassBody().get()) {
        if (bodyDecl.isFieldDeclaration()
            && bodyDecl.asFieldDeclaration().getVariables().stream()
                .anyMatch(v -> v.getNameAsString().equals(expr.toString()))) {
          return bodyDecl.asFieldDeclaration();
        }
      }
    }
    // The current handling of methods in anonymous classes seems to work for now. If an issue
    // arises in the future, add it here.
    // TODO: add method finding based on name and parameter types
    return null;
  }

  /**
   * Returns the resolved declaration of the expression if expression.resolve() throws an
   * UnsupportedOperationException due to its qualifying expression being a constraint type. Returns
   * null if any solving fails.
   *
   * <p>An example: resolving {@code foo.method()}, where {@code foo} is of type {@code ? extends
   * T}. Resolving the method call will fail because T is a type parameter, not a declaration. This
   * could occur in a lambda, where foo is a lambda parameter that is of type {@code ? extends T},
   * but this specific lambda has a type argument for T (like Foo). In this case, we'll want to find
   * the corresponding method declaration from {@code Foo}. A similar case can be found in
   * LambdaBodyStaticUnsolved2Test.
   *
   * <p>This method works under the assumption that the type argument type is solvable; if not, this
   * will return null.
   *
   * @param expression The expression to find the declaration of (method, field)
   * @return The resolved object, if it exists; null otherwise
   */
  public static @Nullable Object tryFindCorrespondingDeclarationForConstraintQualifiedExpression(
      Expression expression) {
    if (!expression.hasScope()) {
      return null;
    }

    Expression scope = ((NodeWithTraversableScope) expression).traverseScope().get();
    ResolvedTypeParameterDeclaration bound;

    ResolvedType resolvedType = Resolver.calculateResolvedType(scope);

    if (resolvedType == null) {
      return null;
    }

    if (resolvedType.isConstraint()) {
      ResolvedLambdaConstraintType constraintType = resolvedType.asConstraintType();
      ResolvedType anyBound = constraintType.getBound();
      if (anyBound.isTypeVariable()) {
        bound = anyBound.asTypeParameter();
      } else {
        return anyBound;
      }
    } else {
      return null;
    }

    LambdaExpr parentLambda = null;
    Node parent = expression.getParentNode().orElse(null);

    while (parent != null) {
      if (parent instanceof LambdaExpr) {
        parentLambda = (LambdaExpr) parent;
        break;
      }
      parent = parent.getParentNode().orElse(null);
    }

    if (parentLambda == null) {
      return null;
    }

    Node parentOfLambda = parentLambda.getParentNode().orElse(null);

    if (parentOfLambda instanceof MethodCallExpr methodCallParent) {
      MethodUsage methodUsage;
      ResolvedMethodDeclaration method;
      try {
        // Must use JavaParserFacade instead of .resolve() here, since we need to get the
        // type parameters map: https://github.com/javaparser/javaparser/issues/2135
        JavaParserFacade parserFacade = JavaParserFacade.get(getTypeSolvers().getTypeSolver());
        methodUsage = parserFacade.solveMethodAsUsage(methodCallParent);
        method = methodUsage.getDeclaration();

        // bound contains the type variable in the declaration of the parameter type, not the type
        // variable in the method

        for (ResolvedType paramType : methodUsage.getParamTypes()) {
          if (!paramType.isReferenceType()) {
            continue;
          }

          for (Pair<ResolvedTypeParameterDeclaration, ResolvedType> typeParamMapPair :
              paramType.asReferenceType().getTypeParametersMap()) {
            // Can't trust bound.getQualifiedName(), because it gives the wrong qualified name
            // if the parameter type declares a type parameter of the same name
            if (typeParamMapPair.a.getName().equals(bound.getName())) {
              if (typeParamMapPair.b.isTypeVariable()) {
                // If the type variable is a type variable, we can use it directly
                bound = typeParamMapPair.b.asTypeParameter();
              } else if (typeParamMapPair.b.isWildcard()
                  && typeParamMapPair.b.asWildcard().getBoundedType().isTypeVariable()) {
                // If the type variable is a wildcard and is bounded, we can use it as well
                bound = typeParamMapPair.b.asWildcard().getBoundedType().asTypeParameter();
              }
              break;
            }
          }
        }
      } catch (UnsolvedSymbolException ex) {
        return null;
      }

      // Using what we already know, let's try to find the value of the implicit type argument

      // First, try to find a Type node that corresponds with the return type of methodCallParent.
      // This could be a variable declarator, a return type in a method declaration, or a parameter
      // type from another declaration.

      // Don't use a map here: in case a ResolvedType is the same for different parameters, we could
      // have different pieces of information (i.e., two parameters are both resolved type T, but
      // the args could be different in the method call)
      List<Pair<ResolvedType, ResolvedType>> resolvedTypeToPotentialASTTypes = new ArrayList<>();

      if (methodCallParent.getParentNode().orElse(null) instanceof ReturnStmt returnStmt
          && returnStmt.getParentNode().orElse(null) instanceof BlockStmt blockStmt
          && blockStmt.getParentNode().orElse(null) instanceof MethodDeclaration methodDecl) {
        ResolvedType returnTypeResolved = Resolver.resolve(methodDecl.getType());

        if (returnTypeResolved != null) {
          resolvedTypeToPotentialASTTypes.add(
              new Pair<>(method.getReturnType(), returnTypeResolved));
        }
      } else if (methodCallParent.getParentNode().orElse(null) instanceof VariableDeclarator varDecl
          && varDecl.getInitializer().isPresent()
          && varDecl.getInitializer().get().equals(methodCallParent)) {
        ResolvedType typeResolved = Resolver.resolve(varDecl.getType());

        if (typeResolved != null) {
          resolvedTypeToPotentialASTTypes.add(new Pair<>(method.getReturnType(), typeResolved));
        }
      } else if (methodCallParent.getParentNode().orElse(null) instanceof AssignExpr assignExpr
          && assignExpr.getValue().equals(methodCallParent)) {
        try {
          ResolvedValueDeclaration resolved = null;
          if (assignExpr.getTarget().isNameExpr()) {
            resolved = Resolver.resolve(assignExpr.getTarget().asNameExpr());
          } else if (assignExpr.getTarget().isFieldAccessExpr()) {
            resolved = Resolver.resolve(assignExpr.getTarget().asFieldAccessExpr());
          }

          if (resolved != null) {
            resolvedTypeToPotentialASTTypes.add(
                new Pair<>(method.getReturnType(), resolved.getType()));
          }
        } catch (UnsolvedSymbolException ex) {
          // getType() may throw
        }
      } else if (methodCallParent.getParentNode().orElse(null) instanceof MethodCallExpr methodCall
          && methodCall.getArguments().contains(methodCallParent)) {
        ResolvedMethodDeclaration methodDecl = Resolver.resolve(methodCall);

        if (methodDecl != null) {
          int argPos = methodCall.getArgumentPosition(methodCallParent);

          try {
            resolvedTypeToPotentialASTTypes.add(
                new Pair<>(method.getReturnType(), methodDecl.getParam(argPos).getType()));
          } catch (UnsolvedSymbolException ex) {
            // getParam().getType() could throw an UnsolvedSymbolException
          }
        }
      }

      for (int i = 0; i < methodCallParent.getArguments().size(); i++) {
        Expression argument = methodCallParent.getArguments().get(i);
        ResolvedType argType = Resolver.calculateResolvedType(argument);
        if (argType != null) {
          resolvedTypeToPotentialASTTypes.add(new Pair<>(method.getParam(i).getType(), argType));
        }
      }

      Map<ResolvedTypeParameterDeclaration, ResolvedType> typeVariableToTypesMap = new HashMap<>();
      for (Pair<ResolvedType, ResolvedType> pair : resolvedTypeToPotentialASTTypes) {
        if (pair.a instanceof ResolvedTypeParameterDeclaration typeVar) {
          typeVariableToTypesMap.put(typeVar, pair.b);
        }

        // If the parameter type is a reference type, then the argument type must also be one
        // Importantly, the type variable names should be the same (but not the values)

        // An example pair might look like this:
        // a: ReferenceType{java.lang.Iterable,
        // typeParametersMap=TypeParametersMap{nameToValue={java.lang.Iterable.T=TypeVariable
        // {JPTypeParameter(T, bounds=[])}}}}
        // b: ReferenceType{java.lang.Iterable,
        // typeParametersMap=TypeParametersMap{nameToValue={java.lang.Iterable.T=ReferenceType{com.example.sql.SqlParserPos, typeParametersMap=TypeParametersMap{nameToValue={}}}}}}

        // In this case, look at the type parameters map and see where the type variables can be
        // mapped (i.e., JPTypeParameter(T) --> SqlParserPos)
        if (pair.a instanceof ResolvedReferenceType refType) {
          ResolvedReferenceType otherRefType = (ResolvedReferenceType) pair.b;

          for (Pair<ResolvedTypeParameterDeclaration, ResolvedType> typeParamMapPair :
              refType.getTypeParametersMap()) {
            Pair<ResolvedTypeParameterDeclaration, ResolvedType> other =
                otherRefType.getTypeParametersMap().stream()
                    .filter(t -> t.a.equals(typeParamMapPair.a))
                    .findFirst()
                    .orElse(null);

            if (other == null) {
              continue;
            }

            if (typeParamMapPair.b.isTypeVariable()) {
              typeVariableToTypesMap.put(typeParamMapPair.b.asTypeParameter(), other.b);
            } else if (typeParamMapPair.b.isWildcard()
                && typeParamMapPair.b.asWildcard().getBoundedType().isTypeVariable()
                && other.b.isWildcard()) {
              typeVariableToTypesMap.put(
                  typeParamMapPair.b.asWildcard().getBoundedType().asTypeParameter(),
                  other.b.asWildcard().getBoundedType());
            }
          }
        }
      }

      ResolvedType type = typeVariableToTypesMap.get(bound);

      if (type instanceof ResolvedReferenceType declaringType
          && declaringType.getTypeDeclaration().isPresent()) {
        if (expression.isMethodCallExpr())
          for (ResolvedMethodDeclaration potentialMethod :
              declaringType.getTypeDeclaration().get().getDeclaredMethods()) {
            if (!potentialMethod
                .getName()
                .equals(expression.asMethodCallExpr().getNameAsString())) {
              continue;
            }

            List<@Nullable ResolvedType> argumentTypes =
                getArgumentTypesAsResolved(expression.asMethodCallExpr().getArguments());

            if (argumentTypes.size() != potentialMethod.getNumberOfParams()) {
              continue;
            }

            boolean match = true;
            for (int i = 0; i < argumentTypes.size(); i++) {
              ResolvedType argType = argumentTypes.get(i);
              ResolvedType paramType = potentialMethod.getParam(i).getType();

              if (argType == null || !paramType.isAssignableBy(argType)) {
                match = false;
                break;
              }
            }

            if (match) {
              return potentialMethod;
            }
          }
      }
    }

    return null;
  }

  /**
   * Given a list, return all subsets.
   *
   * @param <T> The type of the list elements
   * @param original The original list
   * @return A list of all subsets
   */
  public static <T> List<List<T>> generateSubsets(List<T> original) {
    List<List<T>> subsets = new ArrayList<>();
    // There are 2^n - 1 subsets; each bit will determine if an element is included
    int totalSubsets = 1 << original.size();

    for (int i = 0; i < totalSubsets; i++) {
      List<T> subset = new ArrayList<>();
      for (int j = 0; j < original.size(); j++) {
        if ((i & (1 << j)) != 0) {
          subset.add(original.get(j));
        }
      }
      subsets.add(subset);
    }

    return subsets;
  }

  /**
   * Given a list of collections, return all combinations of elements where one element is picked
   * from each collection. For example, if you input a list [[1, 2], [3]], then output [[1, 3], [2,
   * 3]].
   *
   * @param <T> The type of the list elements
   * @param collections The list of collections to combine
   * @return A list of all combinations of elements
   */
  public static <T> List<List<T>> generateAllCombinations(
      List<? extends Collection<T>> collections) {
    List<List<T>> combos = new ArrayList<>();
    combos.add(new ArrayList<>());

    for (Collection<T> set : collections) {
      List<List<T>> newCombos = new ArrayList<>();
      for (List<T> combination : combos) {
        for (T element : set) {
          List<T> newCombination = new ArrayList<>(combination);
          newCombination.add(element);
          newCombos.add(newCombination);
        }
      }
      combos = newCombos;
    }

    return combos;
  }

  /**
   * Same as {@link #generateAllCombinations(List)} but for Lists of Maps instead of Lists of
   * Collections. Each map's entry set is used as the collection of elements to combine.
   *
   * @param <T> The type of the keys in the maps
   * @param <U> The type of the values in the maps
   * @param collections The list of maps to combine
   * @return A list of all combinations of map entries
   */
  public static <T, U> List<List<Map.Entry<T, U>>> generateAllCombinationsForListOfMaps(
      List<? extends Map<T, U>> collections) {
    // AbstractMap.Entry allows null values, unlike Map.Entry
    List<List<Map.Entry<T, U>>> combinable =
        collections.stream()
            .map(
                map ->
                    map.entrySet().stream()
                        .map(
                            entry ->
                                (Map.Entry<T, U>)
                                    new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue()))
                        .toList())
            .toList();
    return generateAllCombinations(combinable);
  }

  /**
   * Finds the closest method or lambda ancestor of a return statement. Will throw if neither is
   * found, since a return statement is always located in one of these contexts.
   *
   * @param returnStmt The return statement
   * @return The closest method or lambda ancestor (either of type MethodDeclaration or LambdaExpr)
   */
  public static Node findClosestMethodOrLambdaAncestor(ReturnStmt returnStmt) {
    @SuppressWarnings("unchecked")
    Node ancestor =
        returnStmt
            .findAncestor(
                n -> n instanceof MethodDeclaration || n instanceof LambdaExpr, Node.class)
            .get();
    return ancestor;
  }

  /**
   * This method handles a very specific case: if an unsolved method call has a scope whose type is
   * a supertype of the type this method is being called in, then it will try to find a method with
   * the same signature in the current type or a solvable supertype.
   *
   * @param methodCall The method call, unsolved
   * @param fqnToCompilationUnit The map of FQNs to compilation units
   * @return A method declaration if found, or null if not
   */
  public static @Nullable MethodDeclaration tryFindMethodDeclarationWithSameSignatureFromThisType(
      MethodCallExpr methodCall, Map<String, CompilationUnit> fqnToCompilationUnit) {
    if (!methodCall.hasScope()) {
      return null;
    }

    Expression scope = methodCall.getScope().get();

    Type scopeType = tryGetTypeFromExpression(scope, fqnToCompilationUnit);

    if (!(scopeType instanceof ClassOrInterfaceType)) {
      return null;
    }

    TypeDeclaration<?> enclosingClass = getEnclosingClassLike(methodCall);

    List<TypeDeclaration<?>> solvableAncestors =
        getAllSolvableAncestors(enclosingClass, fqnToCompilationUnit);

    solvableAncestors.add(enclosingClass);

    boolean isScopeTypeAnAncestor = false;
    for (TypeDeclaration<?> thisOrAncestor : solvableAncestors) {
      if (thisOrAncestor instanceof NodeWithExtends<?> withExtends
          && withExtends.getExtendedTypes().contains(scopeType)) {
        isScopeTypeAnAncestor = true;
        break;
      } else if (thisOrAncestor instanceof NodeWithImplements<?> withImplements
          && withImplements.getImplementedTypes().contains(scopeType)) {
        isScopeTypeAnAncestor = true;
        break;
      }
    }

    if (!isScopeTypeAnAncestor) {
      return null;
    }

    MethodCallExpr clone = methodCall.clone();
    clone.removeScope();
    clone.setParentNode(methodCall.getParentNode().get());

    MethodDeclaration method =
        (MethodDeclaration)
            tryFindSingleCallableForNodeWithUnresolvableArguments(clone, fqnToCompilationUnit);

    clone.remove();

    return method;
  }

  /**
   * Finds the least upper bound given a set of resolved types and solved member types.
   *
   * @param resolvedTypes The resolved types
   * @param solvedMemberTypes The solved member types
   * @return The least upper bound, or null if it is a primitive (this will only be the case if a
   *     primitive is inputted). Note that this is a ResolvedReferenceTypeDeclaration because this
   *     does not consider type variables.
   */
  public static @Nullable ResolvedReferenceTypeDeclaration getLeastUpperBound(
      List<ResolvedType> resolvedTypes, List<SolvedMemberType> solvedMemberTypes) {
    if (resolvedTypes.isEmpty() && solvedMemberTypes.isEmpty()) {
      throw new RuntimeException("No types available to compute least upper bound");
    }

    List<ResolvedReferenceTypeDeclaration> combined = new ArrayList<>();

    for (ResolvedType resolvedType : resolvedTypes) {
      if (!resolvedType.isReferenceType()) {
        // May be a type variable
        continue;
      }

      combined.add(resolvedType.asReferenceType().getTypeDeclaration().get());
    }

    for (SolvedMemberType solvedMemberType : solvedMemberTypes) {
      String fqn = solvedMemberType.getFullyQualifiedNames().iterator().next();
      if (JavaLangUtils.isPrimitive(fqn)) {
        return null;
      }

      try {
        combined.add(getTypeSolvers().getTypeSolver().solveType(fqn));
      } catch (UnsolvedSymbolException e) {
        // Type param, likely
      }
    }

    if (combined.isEmpty()) {
      return null;
    }

    Set<ResolvedReferenceTypeDeclaration> intersectedAncestors =
        Stream.concat(
                combined.get(0).getAllAncestors().stream()
                    .map(anc -> anc.getTypeDeclaration().get()),
                Stream.of(combined.get(0)))
            .collect(Collectors.toCollection(HashSet::new));

    for (ResolvedReferenceTypeDeclaration typeDecl : combined) {
      intersectedAncestors.retainAll(
          Stream.concat(
                  typeDecl.getAllAncestors().stream().map(anc -> anc.getTypeDeclaration().get()),
                  Stream.of(typeDecl))
              .toList());
    }

    return intersectedAncestors.stream()
        .min(
            (a, b) -> {
              if (a.isAssignableBy(b)) return 1;
              if (b.isAssignableBy(a)) return -1;

              // Prefer a class lub to an interface lub
              if (a.isClass() && !b.isClass()) return -1;
              if (!a.isClass() && b.isClass()) return 1;

              return 0;
            })
        .get();
  }

  /**
   * Given a method reference expression, find all possible methods it could be referring to. If the
   * method reference scope type is unsolvable or no methods of matching name could be found (maybe
   * in an unsolved ancestor), then this method returns an empty list.
   *
   * @param methodReference The method reference to find declarations of
   * @return The list of matching resolved method declarations or an empty list if none could be
   *     found
   */
  public static List<? extends ResolvedMethodLikeDeclaration> getMethodDeclarationsFromMethodRef(
      MethodReferenceExpr methodReference) {
    ResolvedType methodDeclaringType = Resolver.calculateResolvedType(methodReference.getScope());

    if (methodDeclaringType == null || !methodDeclaringType.isReferenceType()) {
      return Collections.emptyList();
    }

    String methodName = methodReference.getIdentifier();

    if (methodName.equals("new")) {
      return methodDeclaringType.asReferenceType().getTypeDeclaration().get().getConstructors();
    }

    // Method references must be on reference types
    return methodDeclaringType.asReferenceType().getAllMethods().stream()
        .filter(method -> method.getName().equals(methodName))
        .toList();
  }

  /**
   * Gets an import declaration based on a simple name, if one exists. Returns null if not found.
   *
   * @param name The simple name; does not contain a period
   * @param cu The compilation unit
   * @param mustBeStatic True if looking only for static imports
   * @return The import declaration, if found; if not, then null
   */
  public static @Nullable ImportDeclaration getImportDeclaration(
      String name, CompilationUnit cu, boolean mustBeStatic) {
    for (ImportDeclaration importDecl : cu.getImports()) {
      if (mustBeStatic && !importDecl.isStatic()) {
        continue;
      }

      if (importDecl.getNameAsString().endsWith("." + name)) {
        return importDecl;
      }
    }

    return null;
  }

  /**
   * Given a type declaration, return the resolved method declarations of all must implement
   * methods. Must implement methods are methods that are abstract in JDK superclasses or
   * non-default methods in JDK interfaces.
   *
   * @param typeDecl The type declaration
   * @return The set of resolved JDK method declarations that must be implemented
   */
  public static Set<ResolvedMethodDeclaration> getAllMustImplementMethods(
      TypeDeclaration<?> typeDecl) {
    Set<ResolvedMethodDeclaration> methodsThatMustBeImplemented = new HashSet<>();

    for (ResolvedReferenceTypeDeclaration jdkAncestor : getAllJDKAncestors(typeDecl)) {
      for (ResolvedMethodDeclaration resolvedMethod : jdkAncestor.getDeclaredMethods()) {
        // Skip methods that are already defined in java.lang.Object
        if (JavaLangUtils.isJavaLangObjectMethod(resolvedMethod.getSignature())) {
          continue;
        }

        if (resolvedMethod.isAbstract()) {
          methodsThatMustBeImplemented.add(resolvedMethod);
        }
      }
    }

    return methodsThatMustBeImplemented;
  }

  /**
   * Given a type declaration, return the resolved method declarations of all must implement methods
   * that do not have an existing declaration in the type or its solvable ancestors. Must implement
   * methods are methods that are abstract in JDK superclasses or non-default methods.
   *
   * @param typeDecl The type declaration
   * @param fqnToCompilationUnits A map of fully-qualified type names to their compilation units
   * @return The set of resolved JDK method declarations that must be implemented and do not have an
   *     existing declaration
   */
  public static Set<ResolvedMethodDeclaration> getMustImplementMethodsWithNoExistingDeclaration(
      TypeDeclaration<?> typeDecl, Map<String, CompilationUnit> fqnToCompilationUnits) {
    Set<ResolvedMethodDeclaration> methodsThatMustBeImplemented =
        getAllMustImplementMethods(typeDecl);

    getAllMustImplementMethodsImpl(typeDecl, methodsThatMustBeImplemented, fqnToCompilationUnits);

    return methodsThatMustBeImplemented;
  }

  /**
   * Given a type declaration, return existing definitions of all must implement methods. Must
   * implement methods are methods that are abstract in JDK superclasses or non-default methods in
   * JDK interfaces.
   *
   * @param typeDecl The type declaration to which the parents belong
   * @param nonJDKMustImplements The set of non-JDK must implement methods
   * @param fqnToCompilationUnits A map of fully-qualified type names to their compilation units
   * @return The list of existing method declarations that must be preserved
   */
  public static List<MethodDeclaration> getDeclarationsForAllMustImplementMethods(
      TypeDeclaration<?> typeDecl,
      Set<ResolvedMethodDeclaration> nonJDKMustImplements,
      Map<String, CompilationUnit> fqnToCompilationUnits) {
    Set<ResolvedMethodDeclaration> methodsThatMustBeImplemented =
        getAllMustImplementMethods(typeDecl);

    methodsThatMustBeImplemented.addAll(nonJDKMustImplements);

    return getAllMustImplementMethodsImpl(
        typeDecl, methodsThatMustBeImplemented, fqnToCompilationUnits);
  }

  /**
   * Helper method for getAllMustImplementMethods. Given a set of JDK methods that must be
   * implemented, find the closest method declaration to preserve (i.e., first check this class,
   * then check its parent, then its grandparent, and so on.)
   *
   * @param typeDecl The type declaration
   * @param methodsThatMustBeImplemented A set of methods that must be implemented
   * @param fqnToCompilationUnits A map of fully-qualified type names to their compilation units
   * @return The result list
   */
  private static List<MethodDeclaration> getAllMustImplementMethodsImpl(
      TypeDeclaration<?> typeDecl,
      Set<ResolvedMethodDeclaration> methodsThatMustBeImplemented,
      Map<String, CompilationUnit> fqnToCompilationUnits) {
    List<MethodDeclaration> result = new ArrayList<>();
    for (ResolvedMethodDeclaration resolvedMethodDecl : Set.copyOf(methodsThatMustBeImplemented)) {
      List<List<ResolvedReferenceType>> typesInBetween =
          getTypesInBetween(typeDecl, resolvedMethodDecl.declaringType());

      Set<ResolvedReferenceTypeDeclaration> exploredTypes = new HashSet<>();
      List<List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>>> typeParametersMaps =
          new ArrayList<>();

      MethodDeclaration earliestMethod = null;
      int locationInPath = -1;
      boolean hasJdkDefinition = false;
      for (List<ResolvedReferenceType> path : typesInBetween) {
        if (hasJdkDefinition) {
          break;
        }

        @MonotonicNonNull List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> typeParametersMap = null;
        for (int i = path.size(); i >= 0; i--) {
          ResolvedReferenceTypeDeclaration declaration;

          if (i > 0) {
            ResolvedReferenceType type = path.get(i - 1);
            if (typeParametersMap == null) {
              typeParametersMap = type.getTypeParametersMap();
            } else {
              typeParametersMap =
                  composeTypeParameterMap(type.getTypeParametersMap(), typeParametersMap);
            }
            declaration = type.getTypeDeclaration().get();
          } else {
            if (typeParametersMap == null) {
              typeParametersMap = List.of();
            }
            declaration =
                (ResolvedReferenceTypeDeclaration)
                    Resolver.resolveGuaranteeNonNull((Resolvable<?>) typeDecl);
          }

          exploredTypes.add(declaration);

          // Last in the path will be the type that contains resolvedMethodDecl
          if (i == path.size()) {
            continue;
          }

          for (ResolvedMethodDeclaration resolvedMethod : declaration.getDeclaredMethods()) {
            try {
              if (resolvedMethod
                  .getSignature()
                  .equals(
                      getSignatureFromResolvedMethodWithTypeVariablesMap(
                          resolvedMethodDecl, typeParametersMap))) {

                MethodDeclaration methodDecl =
                    (MethodDeclaration) tryFindAttachedNode(resolvedMethod, fqnToCompilationUnits);

                if (methodDecl != null) {
                  if (!resolvedMethod.isAbstract()) {
                    if (i > locationInPath) {
                      earliestMethod = methodDecl;
                      locationInPath = i;
                    }
                  }
                } else {
                  // We travel the path from the furthest ancestor to the closest ancestor, so if we
                  // find an abstract definition, set hasJdkDefinition to false since the abstract
                  // will override the concrete definition we may have found earlier in the path.
                  hasJdkDefinition = !resolvedMethod.isAbstract();
                }

                if (!resolvedMethod
                    .getQualifiedSignature()
                    .equals(resolvedMethodDecl.getQualifiedSignature())) {
                  methodsThatMustBeImplemented.remove(resolvedMethodDecl);
                }

                break;
              }
            } catch (UnsolvedSymbolException ex) {
              // It's possible that a method could reference an unsolved symbol; in this case, just
              // skip it
            }
          }
        }

        if (typeParametersMap == null) {
          // This error is not possible (satisfy the null checker); the loop above always runs at
          // least once.
          throw new RuntimeException("Impossible");
        }

        typeParametersMaps.add(typeParametersMap);
      }

      if (!hasJdkDefinition) {
        // Maybe there is a method declaration for this, but it's not on the path from the
        // current type declaration to the JDK type declaration.
        // Look at types we haven't explored yet

        List<ResolvedReferenceTypeDeclaration> allSolvableAncestors = new ArrayList<>();
        allSolvableAncestors.addAll(getAllJDKAncestors(typeDecl));
        allSolvableAncestors.addAll(
            getAllSolvableAncestors(typeDecl, fqnToCompilationUnits).stream()
                .map(
                    decl ->
                        (ResolvedReferenceTypeDeclaration)
                            Resolver.resolveGuaranteeNonNull((Resolvable<?>) decl))
                .toList());
        allSolvableAncestors.removeAll(exploredTypes);
        allSolvableAncestors.remove(resolvedMethodDecl.declaringType());

        for (ResolvedReferenceTypeDeclaration ancestor : allSolvableAncestors) {
          if (hasJdkDefinition) {
            break;
          }

          List<List<ResolvedReferenceType>> pathsToAncestor = getTypesInBetween(typeDecl, ancestor);
          // Get the type parameter maps from the declaring type of the method to the current type,
          // then compose this to the path to the ancestor
          for (List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> potentialTypeParamMap :
              typeParametersMaps) {
            if (hasJdkDefinition) {
              break;
            }
            List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> typeParametersMap =
                potentialTypeParamMap;
            for (List<ResolvedReferenceType> path : pathsToAncestor) {
              if (hasJdkDefinition) {
                break;
              }

              for (int i = path.size() - 1; i >= 0; i--) {
                ResolvedReferenceType type = path.get(i);
                typeParametersMap =
                    composeTypeParameterMap(type.getTypeParametersMap(), typeParametersMap);

                for (ResolvedMethodDeclaration resolvedMethod :
                    path.get(i).getTypeDeclaration().get().getDeclaredMethods()) {
                  if (resolvedMethod
                      .getSignature()
                      .equals(
                          getSignatureFromResolvedMethodWithTypeVariablesMap(
                              resolvedMethodDecl, typeParametersMap))) {

                    MethodDeclaration methodDecl =
                        (MethodDeclaration)
                            tryFindAttachedNode(resolvedMethod, fqnToCompilationUnits);

                    if (methodDecl != null) {
                      if (i > locationInPath) {
                        earliestMethod = methodDecl;
                        locationInPath = i;
                      }
                    } else {
                      // We travel the path from the furthest ancestor to the closest ancestor, so
                      // if we find an abstract definition, set hasJdkDefinition to false since
                      // the abstract will override the concrete definition we may have found
                      // earlier in the path.
                      hasJdkDefinition = !resolvedMethod.isAbstract();
                    }

                    if (resolvedMethod.isAbstract()
                        && !resolvedMethod
                            .getQualifiedSignature()
                            .equals(resolvedMethodDecl.getQualifiedSignature())
                        && resolvedMethodDecl.declaringType().isAssignableBy(ancestor)) {
                      methodsThatMustBeImplemented.remove(resolvedMethodDecl);
                    }

                    break;
                  }
                }
              }
            }
          }
        }
      }

      if (!hasJdkDefinition) {
        if (earliestMethod != null) {
          result.add(earliestMethod);
        }
      }
    }

    return result;
  }

  /**
   * If from is A, and to is C, and A <: B and B <: C, then this should return a list of a list
   * which contains B and C. Could contain multiple lists if there are multiple paths to the same
   * type.
   *
   * @param from The first type
   * @param to The last type
   * @return The types in between the from and to types
   */
  private static List<List<ResolvedReferenceType>> getTypesInBetween(
      TypeDeclaration<?> from, ResolvedReferenceTypeDeclaration to) {
    List<List<ResolvedReferenceType>> result = new ArrayList<>();

    List<ClassOrInterfaceType> superTypes = getDirectSuperTypes(from);

    for (ClassOrInterfaceType superType : superTypes) {
      ResolvedType resolvedSuperType = Resolver.resolve(superType);

      if (resolvedSuperType == null) {
        continue;
      }

      ResolvedReferenceType type = resolvedSuperType.asReferenceType();
      getTypesInBetweenImpl(type, to, new ArrayList<>(List.of(type)), result);
    }

    return result;
  }

  /**
   * Helper method for {@link #getTypesInBetween(TypeDeclaration,
   * ResolvedReferenceTypeDeclaration)}.
   *
   * @param from The first type
   * @param to The last type
   * @param accumulator The accumulator for the current path
   * @param result The result list
   */
  private static void getTypesInBetweenImpl(
      ResolvedReferenceType from,
      ResolvedReferenceTypeDeclaration to,
      List<ResolvedReferenceType> accumulator,
      List<List<ResolvedReferenceType>> result) {
    if (to.equals(from.getTypeDeclaration().orElse(null))) {
      result.add(accumulator);
      return;
    }

    try {
      for (ResolvedReferenceType superType : from.getDirectAncestors()) {
        List<ResolvedReferenceType> newAccumulator = new ArrayList<>(accumulator);
        newAccumulator.add(superType);
        getTypesInBetweenImpl(superType, to, newAccumulator, result);
      }
    } catch (UnsolvedSymbolException ex) {
      // getDirectAncestors() may throw
    }
  }

  /**
   * Composes a type parameter map from the previous and new type parameter maps. For example, if
   * previousTypeParametersMap is T --> String and newTypeParametersMap is E --> T, then the
   * composed map should be E --> String.
   *
   * @param previousTypeParametersMap The previous type parameter map
   * @param newTypeParametersMap The new type parameter map
   * @return The composed type parameter map
   */
  private static List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> composeTypeParameterMap(
      List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> previousTypeParametersMap,
      List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> newTypeParametersMap) {
    List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> result = new ArrayList<>();
    for (Pair<ResolvedTypeParameterDeclaration, ResolvedType> entry : newTypeParametersMap) {
      // In the above example, this would be T in E --> T
      ResolvedType typeToReplace = entry.b;

      if (typeToReplace.isTypeVariable()) {
        ResolvedTypeParameterDeclaration typeVar = typeToReplace.asTypeVariable().asTypeParameter();
        for (Pair<ResolvedTypeParameterDeclaration, ResolvedType> pair :
            previousTypeParametersMap) {
          if (pair.a.equals(typeVar)) {
            typeToReplace = pair.b;
            break;
          }
        }
      }
      result.add(new Pair<>(entry.a, typeToReplace));
    }

    return result;
  }

  /**
   * Given a resolved method declaration and its declaring type's type variables map, return the
   * method's signature with the type variables replaced by their resolved types.
   *
   * <p>For example, if the method is part of {@code Foo<T>} and the type variables map is {@code T
   * --> String}, then any parameters that match T will be replaced with String in the signature.
   *
   * <p>This method may throw an UnsolvedSymbolException because of a call to {@link
   * ResolvedParameterDeclaration#getType()}.
   *
   * @param method The resolved method declaration in the generic class
   * @param typeVariablesMap The type variables map, which maps type variable declarations to their
   *     resolved types
   * @return The method's signature with the type variables replaced by their resolved types
   */
  public static String getSignatureFromResolvedMethodWithTypeVariablesMap(
      ResolvedMethodDeclaration method,
      List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> typeVariablesMap) {
    StringBuilder signature = new StringBuilder(method.getName() + "(");

    for (int i = 0; i < method.getNumberOfParams(); i++) {
      ResolvedParameterDeclaration param = method.getParam(i);

      signature.append(getResolvedNameWithSubstitution(param.getType(), typeVariablesMap));

      if (i < method.getNumberOfParams() - 1) {
        signature.append(", ");
      }
    }

    signature.append(")");

    return signature.toString();
  }

  /**
   * Gets the resolved name of a type, substituting any type variables with their resolved types
   *
   * @param type The type to get the name of
   * @param typeVariablesMap The type variables map
   * @return The resolved name of the type, with type variables substituted
   */
  private static String getResolvedNameWithSubstitution(
      ResolvedType type,
      List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> typeVariablesMap) {
    if (type.isTypeVariable()) {
      for (Pair<ResolvedTypeParameterDeclaration, ResolvedType> pair : typeVariablesMap) {
        if (pair.a.equals(type.asTypeVariable().asTypeParameter())) {
          return pair.b.describe();
        }
      }
      return type.asTypeVariable().describe();
    }

    if (type.isWildcard()) {
      ResolvedWildcard wildcard = type.asWildcard();
      if (!wildcard.isBounded()) {
        return "?";
      }
      String bound = getResolvedNameWithSubstitution(wildcard.getBoundedType(), typeVariablesMap);
      return wildcard.isExtends() ? "? extends " + bound : "? super " + bound;
    }

    if (type.isReferenceType()) {
      ResolvedReferenceType ref = type.asReferenceType();
      StringBuilder sb = new StringBuilder(ref.getQualifiedName());

      List<ResolvedType> typeArgs = ref.typeParametersValues();
      if (!typeArgs.isEmpty()) {
        sb.append("<");
        for (int i = 0; i < typeArgs.size(); i++) {
          sb.append(getResolvedNameWithSubstitution(typeArgs.get(i), typeVariablesMap));
          if (i < typeArgs.size() - 1) sb.append(", ");
        }
        sb.append(">");
      }

      return sb.toString();
    }

    if (type.isArray()) {
      return getResolvedNameWithSubstitution(
              type.asArrayType().getComponentType(), typeVariablesMap)
          + "[]";
    }

    return type.describe();
  }

  /**
   * Returns true if the given type or any of its outer types are private.
   *
   * @param fqn The FQN of the type to check
   * @param fqnToCompilationUnits A map of FQNs to compilation units
   * @return True if the type or any of its outer types are private, false otherwise
   */
  public static boolean areTypeOrOuterTypesPrivate(
      String fqn, Map<String, CompilationUnit> fqnToCompilationUnits) {
    TypeDeclaration<?> typeDecl = getTypeFromQualifiedName(fqn, fqnToCompilationUnits);
    if (typeDecl != null) {
      if (typeDecl.isPrivate()) {
        return true;
      }

      int lastIndexOfDot = fqn.lastIndexOf('.');
      if (lastIndexOfDot == -1) {
        return false;
      }

      TypeDeclaration<?> outerType = getEnclosingClassLikeOptional(typeDecl);

      while (outerType != null) {
        // A non-public inner class is private.
        if (!typeDecl.isPublic()) {
          return true;
        }

        typeDecl = outerType;
        outerType = getEnclosingClassLikeOptional(outerType);
      }
    }
    return false;
  }

  /**
   * Given a class or interface declaration and a class or interface type, generate a map of type
   * parameter names to their resolved types. {@code from} is the subtype; {@code to} is the
   * ancestor in question. The resulting map will contain keys that map to the {@code from}'s type
   * parameters to their values in the context of {@code to}.
   *
   * @param from The class or interface declaration that is the subtype
   * @param to The class or interface declaration that is the ancestor
   * @return A map of type parameter names to their resolved types
   */
  public static Map<String, String> generateTypeParameterMap(
      ClassOrInterfaceDeclaration from, ClassOrInterfaceDeclaration to) {
    if (from.equals(to)) {
      return from.getTypeParameters().stream()
          .collect(
              Collectors.toMap(TypeParameter::getNameAsString, TypeParameter::getNameAsString));
    }

    List<ResolvedReferenceType> path =
        getTypesInBetween(from, Resolver.resolveGuaranteeNonNull(to)).get(0);

    // Foo<A, B> extends Bar<A, B> --> Bar<C, D> extends Baz<C, String> --> Baz<E, F> extends some
    // Unsolved<E, F>

    // tpm: {E --> C, F --> String}
    // tpm: {C --> A, D --> B}

    // We want A --> E, B --> D, but this code will give us E --> A, F --> B. So we need to reverse
    // the map at the end.

    @MonotonicNonNull List<Pair<ResolvedTypeParameterDeclaration, ResolvedType>> typeParametersMap = null;
    for (int i = path.size(); i >= 0; i--) {
      if (i > 0) {
        ResolvedReferenceType type = path.get(i - 1);
        if (typeParametersMap == null) {
          typeParametersMap = type.getTypeParametersMap();
        } else {
          typeParametersMap =
              composeTypeParameterMap(type.getTypeParametersMap(), typeParametersMap);
        }
      } else if (typeParametersMap == null) {
        typeParametersMap = List.of();
      }
    }

    return typeParametersMap == null
        ? Map.of()
        : typeParametersMap.stream()
            .collect(Collectors.toMap(pair -> pair.b.describe(), pair -> pair.a.getName()));
  }

  /**
   * Given an index, return a generated type parameter name. For example, if the index is 0, return
   * "T". If the index is 1, return "T1".
   *
   * @param index The index of the type parameter
   * @return The generated type parameter name
   */
  public static String getGeneratedTypeParameterName(int index) {
    return "T" + ((index > 0) ? index : "");
  }
}
