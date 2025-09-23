package org.checkerframework.specimin;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
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
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.AssociableToAST;
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
import com.github.javaparser.utils.Pair;
import com.google.common.base.Splitter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;
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
   * Set this TypeSolver instance to be used by the JavaParserFacade, so we don't have to find it
   * through reflection in JavaParserSymbolSolver. This field is initialized once in SpeciminRunner.
   * Note that by the time you evaluate the value of this field, it should already be non-null.
   */
  private static @MonotonicNonNull TypeSolver typeSolver = null;

  /**
   * Set the TypeSolver instance to be used by the JavaParserFacade.
   *
   * @param typeSolver the TypeSolver instance to set
   */
  @EnsuresNonNull("JavaParserUtil.typeSolver")
  public static void setTypeSolver(TypeSolver typeSolver) {
    JavaParserUtil.typeSolver = typeSolver;
  }

  /**
   * Gets the type solver, and ensures it is non-null.
   *
   * @return The type solver
   */
  public static TypeSolver getTypeSolver() {
    if (typeSolver == null) {
      throw new RuntimeException(
          "TypeSolver is not set. Make sure to call setTypeSolver() in SpeciminRunner.");
    }
    return typeSolver;
  }

  /**
   * Removes a node from its compilation unit. If a node cannot be removed directly, it might be
   * wrapped inside another node, causing removal failure. This method iterates through the parent
   * nodes until it successfully removes the specified node.
   *
   * <p>If this explanation does not make sense to you, please refer to the following link for
   * further details: <a
   * href="https://github.com/javaparser/javaparser/issues/858">https://github.com/javaparser/javaparser/issues/858</a>
   *
   * @param node The node to be removed.
   */
  public static void removeNode(Node node) {
    while (!node.remove()) {
      node = node.getParentNode().get();
    }
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
    // should be lower case. If otherwise, then it may be a constant
    Character first = string.charAt(0);
    if (string.length() > 1) {
      Character second = string.charAt(1);

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
   * Given a ClassOrInterfaceType instance, this method returns a corresponding
   * ResolvedReferenceType. This method might throw an exception if the given instance is
   * unresolved.
   *
   * <p>Note: In JavaParser, ClassOrInterfaceType is a subtype of ReferenceType (check an example
   * here:
   * https://github.com/javaparser/javaparser/blob/9c133d19d5b85b3b758f05762fb4d7c9875ef681/javaparser-core/src/main/java/com/github/javaparser/ast/type/ClassOrInterfaceType.java#L258).
   * However, the resolve() method in ClassOrInterfaceType only returns a ResolvedType instead of a
   * specific ResolvedReferenceType. This appears to be an inaccuracy within JavaParser's type
   * hierarchy.
   *
   * @param type the ClassOrInterfaceType instance
   * @return the corresponding ResolvedReferenceType instance
   */
  public static ResolvedReferenceType classOrInterfaceTypeToResolvedReferenceType(
      ClassOrInterfaceType type) {
    return type.resolve().asReferenceType();
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
   * Returns the corresponding type name for an Expression within an annotation.
   *
   * @param value The value to evaluate the type of
   * @return The corresponding type name for the value: constrained to a primitive type, String,
   *     Class&lt;?&gt;, an enum, an annotation, or an array of any of those types, as per
   *     annotation parameter requirements.
   */
  public static String getValueTypeFromAnnotationExpression(Expression value) {
    if (value.isBooleanLiteralExpr()) {
      return "boolean";
    } else if (value.isStringLiteralExpr()) {
      return "String";
    } else if (value.isIntegerLiteralExpr()) {
      return "int";
    } else if (value.isLongLiteralExpr()) {
      return "long";
    } else if (value.isDoubleLiteralExpr()) {
      return "double";
    } else if (value.isCharLiteralExpr()) {
      return "char";
    } else if (value.isArrayInitializerExpr()) {
      ArrayInitializerExpr array = value.asArrayInitializerExpr();
      if (!array.getValues().isEmpty()) {
        Expression firstElement = array.getValues().get(0);
        return getValueTypeFromAnnotationExpression(firstElement) + "[]";
      }
      // Handle empty arrays (i.e. @Anno({})); we have no way of telling
      // what it actually is
      return "String[]";
    } else if (value.isAnnotationExpr()) {
      return value.asAnnotationExpr().getNameAsString();
    } else if (value.isFieldAccessExpr()) {
      // Enums are FieldAccessExprs (Enum.SOMETHING)
      return value.asFieldAccessExpr().getScope().toString();
    } else if (value.isClassExpr()) {
      // Handle all classes
      return "Class<?>";
    } else if (value.isNameExpr()) {
      // Constant/variable
      try {
        ResolvedType resolvedType = value.asNameExpr().calculateResolvedType();

        if (resolvedType.isPrimitive()) {
          return resolvedType.asPrimitive().describe();
        } else if (resolvedType.isReferenceType()) {
          return resolvedType.asReferenceType().getQualifiedName();
        } else {
          return resolvedType.describe();
        }
      } catch (UnsolvedSymbolException ex) {
        return value.toString();
      }
    }
    return value.toString();
  }

  /**
   * Searches the ancestors of the given node until it finds a class or interface node, and then
   * returns the fully-qualified name of that class or interface.
   *
   * <p>This method will fail if it is called on a node that is not contained in a class or
   * interface.
   *
   * @param node a node contained in a class or interface
   * @return the fully-qualified name of the inner-most containing class or interface
   */
  @SuppressWarnings("signature") // result is a fully-qualified name or else this throws
  public static @FullyQualifiedName String getEnclosingClassName(Node node) {
    return getEnclosingClassLike(node).getFullyQualifiedName().orElseThrow();
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
   * <p>This is also safe to call on non qualified names as well; it simply returns the input.
   *
   * @param qualified The qualified class name
   * @return The simple class name
   */
  public static String getSimpleNameFromQualifiedName(String qualified) {
    return qualified.substring(qualified.lastIndexOf('.') + 1);
  }

  /**
   * Returns a package prefix that can be prepended to a class name, for a given method or
   * constructor declaration.
   *
   * @param decl declaration to extract a package prefix from(using getPackageName)
   * @return empty string if default package, otherwise package name followed by "."
   */
  public static String packagePrefix(ResolvedMethodLikeDeclaration decl) {
    String packageName = decl.getPackageName();
    return packageName.isEmpty() ? "" : packageName + ".";
  }

  /**
   * Returns true iff the innermost enclosing class/interface is an enum.
   *
   * @param node any node
   * @return true if the enclosing class is an enum, false otherwise
   */
  public static boolean isInEnum(Node node) {
    Optional<Node> parent = node.getParentNode();
    while (parent.isPresent()) {
      Node actualParent = parent.get();
      if (actualParent instanceof EnumDeclaration) {
        return true;
      }
      parent = actualParent.getParentNode();
    }
    return false;
  }

  /**
   * Finds the closest method, field, or class-like declaration (enums, annos)
   *
   * @param node The node to find the parent for
   * @return the Node of the closest member or class declaration
   */
  public static Node findClosestParentMemberOrClassLike(Node node) {
    Node parent = node.getParentNode().orElseThrow();
    while (!(parent instanceof ClassOrInterfaceDeclaration
        || parent instanceof EnumDeclaration
        || parent instanceof AnnotationDeclaration
        || parent instanceof ConstructorDeclaration
        || parent instanceof MethodDeclaration
        || parent instanceof FieldDeclaration)) {
      parent = parent.getParentNode().orElseThrow();
    }
    return parent;
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
   * Given a method or constructor declaration, this method returns the declaration of that method
   * without the return type, internal spaces, and any possible annotation.
   *
   * @param decl the declaration to be used as input
   * @return decl without the return type and any possible annotation.
   */
  public static String removeMethodReturnTypeSpacesAndAnnotations(NodeWithDeclaration decl) {
    String declAsString = decl.getDeclarationAsString(false, false, false);
    return removeMethodReturnTypeAndAnnotationsImpl(declAsString).replaceAll("\\s", "");
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
    String result = methodWithoutReturnType.replace("< ", "<");
    return result;
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
        try {
          ResolvedType scopeType = expr.calculateResolvedType();

          if (getSimpleNameFromQualifiedName(scopeType.describe()).equals(scope.toString())) {
            return scopeType.describe() + "." + nameOfExpr;
          }
        } catch (UnsolvedSymbolException ex) {
          // continue
        }

        nameOfScope = scope.asNameExpr().getNameAsString();
      } else if (scope.isFieldAccessExpr()) {
        try {
          ResolvedType scopeType = expr.calculateResolvedType();

          if (getSimpleNameFromQualifiedName(scopeType.describe()).endsWith(scope.toString())) {
            return scopeType.describe() + "." + nameOfExpr;
          }
        } catch (UnsolvedSymbolException ex) {
          // continue
        }

        nameOfScope = scope.asFieldAccessExpr().toString();
        if (isAClassPath(nameOfScope)) {
          return nameOfScope + "." + nameOfExpr;
        }
      } else {
        return null;
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
   * Returns true if the expression type is resolvable; i.e., {@code calculateResolvedType()} runs
   * without an {@code UnsolvedSymbolException}.
   *
   * @param expr The expression
   * @return True if the expression is resolvable
   */
  public static boolean isExprTypeResolvable(Expression expr) {
    try {
      expr.calculateResolvedType();
      return true;
    } catch (UnsolvedSymbolException | UnsupportedOperationException ex) {
      // We can get an UnsupportedOperationException when trying to resolve an unsolvable method
      // reference
      return false;
    }
  }

  /**
   * Returns true if this expression is resolvable to a definition.
   *
   * @param expr The expression to resolve
   * @return True if this expression is of type {@code Resolvable<?>} and also has a resolvable
   *     definition
   */
  public static boolean isExprDefinitionResolvable(Expression expr) {
    if (!(expr instanceof Resolvable<?> resolvable)) {
      return false;
    }

    try {
      resolvable.resolve();
      return true;
    } catch (UnsolvedSymbolException ex) {
      return false;
    } catch (UnsupportedOperationException ex) {
      if (tryFindCorrespondingDeclarationForConstraintQualifiedExpression(expr) != null) {
        return true;
      }
      return false;
    }
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
    try {
      if (expression.isNameExpr()) {
        ResolvedValueDeclaration resolved = expression.asNameExpr().resolve();

        return getTypeFromResolvedValueDeclaration(resolved, fqnToCompilationUnits);
      } else if (expression.isFieldAccessExpr()) {
        ResolvedValueDeclaration resolved = expression.asFieldAccessExpr().resolve();

        return getTypeFromResolvedValueDeclaration(resolved, fqnToCompilationUnits);
      } else if (expression.isMethodCallExpr()) {
        ResolvedMethodDeclaration resolved = expression.asMethodCallExpr().resolve();

        if (resolved.toAst().isPresent()) {
          MethodDeclaration methodDecl = (MethodDeclaration) resolved.toAst().get();
          return methodDecl.getType();
        }
      }
    } catch (UnsolvedSymbolException ex) {
      return null;
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
    try {
      return expression.calculateResolvedType().describe();
    } catch (UnsolvedSymbolException ex) {
      // continue
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
   * Removes array brackets from a type name. i.e., int[][][] -> int
   *
   * @param name The name of the type
   * @return The name of the type, without the array brackets
   */
  public static String removeArrayBrackets(String name) {
    return name.replaceAll("(\\[\\])+$", "");
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

    try {
      ResolvedType type = constructorCall.getType().resolve();

      enclosingClass = getTypeFromQualifiedName(type.describe(), fqnToCompilationUnits);

      if (enclosingClass == null) {
        return List.of();
      }
    } catch (UnsolvedSymbolException ex) {
      // not relevant
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
      try {
        parent =
            getTypeFromQualifiedName(
                getSuperClass(constructorCall).resolve().describe(), fqnToCompilationUnits);
      } catch (UnsolvedSymbolException ex) {
        // continue
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

    List<TypeDeclaration<?>> enclosingClass = new ArrayList<>();
    if (methodCall.hasScope()) {
      Expression scope = methodCall.getScope().get();

      if (scope.isSuperExpr()) {
        isSuperOnly = true;
      }

      try {
        ResolvedType scopeType = scope.calculateResolvedType();

        if (scopeType.isTypeVariable()) {
          for (Bound bound : scopeType.asTypeParameter().getBounds()) {
            TypeDeclaration<?> decl =
                getTypeFromQualifiedName(bound.getType().describe(), fqnToCompilationUnits);

            if (decl != null) {
              enclosingClass.add(decl);
            }
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
      } catch (UnsolvedSymbolException ex) {
        // Maybe the scope has type arguments; try to resolve without those.
        String scopeType =
            getQualifiedNameOfTypeOfExpressionWithUnresolvableTypeArgs(
                scope, fqnToCompilationUnits);

        if (scopeType == null) {
          return List.of();
        }

        TypeDeclaration<?> decl = getTypeFromQualifiedName(scopeType, fqnToCompilationUnits);

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

    try {
      ResolvedType type = enumConstant.resolve().getType();

      enclosingClass = getTypeFromQualifiedName(type.describe(), fqnToCompilationUnits);

      if (enclosingClass == null) {
        return List.of();
      }
    } catch (UnsolvedSymbolException ex) {
      // not relevant
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
    Object resolved = null;

    try {
      resolved = resolvable.resolve();
    } catch (UnsolvedSymbolException ex) {
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

    try {
      ResolvedType resolvedType = classOrInterfaceType.resolve();
      if (typeArgs.isPresent()) {
        classOrInterfaceType.setTypeArguments(typeArgs.get());
      }
      return resolvedType.describe();
    } catch (UnsolvedSymbolException ex2) {
      if (typeArgs.isPresent()) {
        classOrInterfaceType.setTypeArguments(typeArgs.get());
      }
      return null;
    }
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
    List<@Nullable ResolvedType> parameterTypes = new ArrayList<>();

    for (Expression argument : arguments) {
      try {
        parameterTypes.add(argument.calculateResolvedType());
      } catch (UnsolvedSymbolException ex) {
        parameterTypes.add(null);
      }
    }

    return parameterTypes;
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

        try {
          ResolvedType resolvedParameterType = callable.getParameter(i).resolve().getType();

          if (typeInCall == null) {
            continue;
          }

          if (resolvedParameterType.isReferenceType() && typeInCall.isReferenceType()) {
            if (!resolvedParameterType.isAssignableBy(typeInCall)) {
              isAMatch = false;
              break;
            }
          }
        } catch (UnsolvedSymbolException ex) {
          if (typeInCall != null) {
            isAMatch = false;
            break;
          }
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
      try {
        ResolvedType resolvedType = type.resolve();
        TypeDeclaration<?> typeDecl =
            getTypeFromQualifiedName(resolvedType.describe(), fqnToCompilationUnits);

        if (typeDecl == null) {
          continue;
        }

        getAllUnsolvableAncestorsImpl(typeDecl, fqnToCompilationUnits, result);
      } catch (UnsolvedSymbolException ex) {
        result.add(type);
      }
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

    try {
      ResolvedReferenceTypeDeclaration resolved = start.resolve();
      getAllJDKAncestorsImpl(resolved, result);
    } catch (UnsolvedSymbolException ex) {
      // continue
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
      try {
        ResolvedType resolvedType = type.resolve();
        TypeDeclaration<?> typeDecl =
            getTypeFromQualifiedName(resolvedType.describe(), fqnToCompilationUnits);

        if (typeDecl == null) {
          continue;
        }

        result.add(typeDecl);
        getAllSolvableAncestorsImpl(typeDecl, fqnToCompilationUnits, result);
      } catch (UnsolvedSymbolException ex) {
        // continue
      }
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

    TypeDeclaration<?> type =
        someCandidate
            .findFirst(
                TypeDeclaration.class,
                n ->
                    n.getFullyQualifiedName().isPresent()
                        && n.getFullyQualifiedName().get().equals(erased))
            .orElse(null);

    return type;
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

    return attached.findFirst(detachedNode.getClass(), n -> n.equals(detachedNode)).get();
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
   * Given an expression that may be in an anonymous class, try to resolve it. If it is not in an
   * anonymous class or cannot be resolved, return null.
   *
   * @param expression The expression to resolve
   * @return The resolved value, or null if not found
   */
  public static @Nullable Object tryResolveExpressionIfInAnonymousClass(Expression expression) {
    ObjectCreationExpr anonymousClassDecl = getEnclosingAnonymousClassIfExists(expression);

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

    // Temporarily insert a copy of the expression outside of the anonymous class and
    // see if it is resolvable
    Expression copy = expression.clone();
    copy.setParentNode(current.getParentNode().get());

    if (copy instanceof Resolvable<?> resolvable) {
      try {
        Object result = resolvable.resolve();
        copy.remove();
        return result;
      } catch (UnsolvedSymbolException e) {
        // Go below and try to see if calculateResolvedType works
      }
    }

    try {
      Object result = copy.calculateResolvedType();
      copy.remove();
      return result;
    } catch (RuntimeException e) {
      // A RuntimeException can also occur when we try to call calculateResolvedType.
      // UnsolvedSymbolException is caught by RuntimeException.
    }

    copy.remove();
    return null;
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
    if (anonymousClass.getAnonymousClassBody().isPresent()) {
      if (expr.isNameExpr()
          || (expr.isFieldAccessExpr() && expr.asFieldAccessExpr().getScope().isThisExpr())) {
        // Try to find the field
        for (BodyDeclaration<?> bodyDecl : anonymousClass.getAnonymousClassBody().get()) {
          if (bodyDecl.isFieldDeclaration()) {
            FieldDeclaration fieldDecl = bodyDecl.asFieldDeclaration();
            return fieldDecl;
          }
        }
      }
      // The current handling of methods in anonymous classes seems to work for now. If an issue
      // arises in the future, add it here.
      // TODO: add method finding based on name and parameter types
    }
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

    try {
      ResolvedType resolvedType = scope.calculateResolvedType();
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
    } catch (UnsolvedSymbolException ex) {
      return null;
    }

    LambdaExpr parentLambda = null;
    Node parent = expression.getParentNode().orElse(null);

    while (parent != null && parentLambda == null) {
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
        JavaParserFacade parserFacade = JavaParserFacade.get(getTypeSolver());
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
        try {
          ResolvedType returnTypeResolved = methodDecl.getType().resolve();

          resolvedTypeToPotentialASTTypes.add(
              new Pair<>(method.getReturnType(), returnTypeResolved));
        } catch (UnsolvedSymbolException ex) {
          // continue
        }
      } else if (methodCallParent.getParentNode().orElse(null) instanceof VariableDeclarator varDecl
          && varDecl.getInitializer().isPresent()
          && varDecl.getInitializer().get().equals(methodCallParent)) {
        try {
          ResolvedType typeResolved = varDecl.getType().resolve();

          resolvedTypeToPotentialASTTypes.add(new Pair<>(method.getReturnType(), typeResolved));
        } catch (UnsolvedSymbolException ex) {
          // continue
        }
      } else if (methodCallParent.getParentNode().orElse(null) instanceof AssignExpr assignExpr
          && assignExpr.getValue().equals(methodCallParent)) {
        try {
          if (assignExpr.getTarget().isNameExpr()) {
            resolvedTypeToPotentialASTTypes.add(
                new Pair<>(
                    method.getReturnType(),
                    assignExpr.getTarget().asNameExpr().resolve().getType()));
          } else if (assignExpr.getTarget().isFieldAccessExpr()) {
            resolvedTypeToPotentialASTTypes.add(
                new Pair<>(
                    method.getReturnType(),
                    assignExpr.getTarget().asFieldAccessExpr().resolve().getType()));
          }
        } catch (UnsolvedSymbolException ex) {
          // continue
        }
      } else if (methodCallParent.getParentNode().orElse(null) instanceof MethodCallExpr methodCall
          && methodCall.getArguments().contains(methodCallParent)) {
        try {
          ResolvedMethodDeclaration methodDecl = methodCall.resolve();

          int argPos = methodCall.getArgumentPosition(methodCallParent);

          resolvedTypeToPotentialASTTypes.add(
              new Pair<>(method.getReturnType(), methodDecl.getParam(argPos).getType()));
        } catch (UnsolvedSymbolException ex) {
          // continue
        }
      }

      for (int i = 0; i < methodCallParent.getArguments().size(); i++) {
        Expression argument = methodCallParent.getArguments().get(i);
        if (isExprTypeResolvable(argument)) {
          ResolvedType argType = argument.calculateResolvedType();
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
                        .<Map.Entry<T, U>>map(
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
            .<Node>findAncestor(
                n -> {
                  return n instanceof MethodDeclaration || n instanceof LambdaExpr;
                },
                Node.class)
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

    if (scopeType == null) {
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

    if (method != null) {
      return method;
    }

    return null;
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
        combined.add(getTypeSolver().solveType(fqn));
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
    if (!isExprTypeResolvable(methodReference.getScope())) {
      return Collections.emptyList();
    }

    ResolvedType methodDeclaringType = methodReference.getScope().calculateResolvedType();

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
   * @param fqnToCompilationUnits A map of fully-qualified type names to their compilation units
   * @return The list of existing method declarations that must be preserved
   */
  public static List<MethodDeclaration> getDeclarationsForAllMustImplementMethods(
      TypeDeclaration<?> typeDecl, Map<String, CompilationUnit> fqnToCompilationUnits) {
    Set<ResolvedMethodDeclaration> methodsThatMustBeImplemented =
        getAllMustImplementMethods(typeDecl);

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
            declaration = typeDecl.resolve();
          }

          exploredTypes.add(declaration);

          // Last in the path will be the type that contains resolvedMethodDecl
          if (i == path.size()) {
            continue;
          }

          for (ResolvedMethodDeclaration resolvedMethod : declaration.getDeclaredMethods()) {
            if (resolvedMethod
                .getSignature()
                .equals(
                    getSignatureFromResolvedMethodWithTypeVariablesMap(
                        resolvedMethodDecl, typeParametersMap))) {

              if (!resolvedMethod.isAbstract()) {
                MethodDeclaration methodDecl =
                    (MethodDeclaration) tryFindAttachedNode(resolvedMethod, fqnToCompilationUnits);

                if (methodDecl != null) {
                  if (i > locationInPath) {
                    earliestMethod = methodDecl;
                    locationInPath = i;
                  }
                } else {
                  hasJdkDefinition = true;
                }
              }

              if (!resolvedMethod
                  .getQualifiedSignature()
                  .equals(resolvedMethodDecl.getQualifiedSignature())) {
                methodsThatMustBeImplemented.remove(resolvedMethodDecl);
              }

              break;
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
                .map(anc -> anc.resolve())
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

                    if (!resolvedMethod.isAbstract()) {
                      MethodDeclaration methodDecl =
                          (MethodDeclaration)
                              tryFindAttachedNode(resolvedMethod, fqnToCompilationUnits);

                      if (methodDecl != null) {
                        if (i > locationInPath) {
                          earliestMethod = methodDecl;
                          locationInPath = i;
                        }
                      } else {
                        hasJdkDefinition = true;
                      }

                      methodsThatMustBeImplemented.remove(resolvedMethodDecl);
                    } else if (!resolvedMethod
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
      try {
        ResolvedReferenceType type = superType.resolve().asReferenceType();
        getTypesInBetweenImpl(type, to, new ArrayList<>(List.of(type)), result);
      } catch (UnsolvedSymbolException e) {
        // continue
      }
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

    for (ResolvedReferenceType superType : from.getDirectAncestors()) {
      List<ResolvedReferenceType> newAccumulator = new ArrayList<>(accumulator);
      newAccumulator.add(superType);
      getTypesInBetweenImpl(superType, to, newAccumulator, result);
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
   * <p>For example, if the method is part of Foo<T> and the type variables map is T --> String,
   * then any parameters that match T will be replaced with String in the signature.
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

        return true;
      }
    }
    return false;
  }
}
