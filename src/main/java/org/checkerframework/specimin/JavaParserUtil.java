package org.checkerframework.specimin;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.AnnotationDeclaration;
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
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithExtends;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;

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
    TypeDeclaration<?> decl = JavaParserUtil.getEnclosingClassLike(node);

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
    } catch (UnsolvedSymbolException ex) {
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
   * common Java package naming conventions; i.e., all lowercase letters, underscores, and dots.
   *
   * @param type The type/package name
   * @return True if the type is probably a package
   */
  public static boolean isProbablyAPackage(String type) {
    // If all characters are lowercase, a period, or an underscore, it is probably a package
    // https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html
    for (int i = 0; i < type.length(); i++) {
      char c = type.charAt(i);
      if (c != '.' && c != '_' && !Character.isLowerCase(c)) {
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
   * Given a constructor call, returns all possible constructors that match the arity and known
   * types of the arguments.
   *
   * @param constructorCall The constructor call expression
   * @return All possible constructor declarations
   */
  public static List<ConstructorDeclaration> tryResolveConstructorCallWithUnresolvableArguments(
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
   * types of the arguments.
   *
   * @param constructorCall The constructor call statement (super or this constructor call)
   * @return All possible constructor declarations
   */
  public static List<ConstructorDeclaration> tryResolveConstructorCallWithUnresolvableArguments(
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
      TypeDeclaration<?> ancestor = null;
      try {
        ancestor =
            getTypeFromQualifiedName(
                getSuperClass(constructorCall).resolve().describe(), fqnToCompilationUnits);
      } catch (UnsolvedSymbolException ex) {
        // continue
      }

      if (ancestor != null) {
        addAllMatchingCallablesToList(
            ancestor, parameterTypes, candidates, null, ConstructorDeclaration.class);
      }
    }

    return candidates;
  }

  /**
   * Given a method call, returns all possible methods that match the arity and known types of the
   * arguments.
   *
   * @param methodCall The method call expression
   * @return All possible method declarations
   */
  public static List<MethodDeclaration> tryResolveMethodCallWithUnresolvableArguments(
      MethodCallExpr methodCall, Map<String, CompilationUnit> fqnToCompilationUnits) {
    boolean isSuperOnly = false;

    TypeDeclaration<?> enclosingClass;
    if (methodCall.hasScope()) {
      Expression scope = methodCall.getScope().get();

      if (scope.isSuperExpr()) {
        isSuperOnly = true;
      }

      try {
        ResolvedType scopeType = scope.calculateResolvedType();

        enclosingClass = getTypeFromQualifiedName(scopeType.describe(), fqnToCompilationUnits);

        if (enclosingClass == null) {
          return List.of();
        }
      } catch (UnsolvedSymbolException ex) {
        // not relevant
        return List.of();
      }
    } else {
      enclosingClass = JavaParserUtil.getEnclosingClassLike(methodCall);
    }

    List<@Nullable ResolvedType> parameterTypes =
        getArgumentTypesAsResolved(methodCall.getArguments());

    List<MethodDeclaration> candidates = new ArrayList<>();

    if (!isSuperOnly) {
      addAllMatchingCallablesToList(
          enclosingClass,
          parameterTypes,
          candidates,
          methodCall.getNameAsString(),
          MethodDeclaration.class);
    }

    for (TypeDeclaration<?> ancestor :
        getAllSolvableAncestors(enclosingClass, fqnToCompilationUnits)) {
      addAllMatchingCallablesToList(
          ancestor,
          parameterTypes,
          candidates,
          methodCall.getNameAsString(),
          MethodDeclaration.class);
    }

    return candidates;
  }

  /**
   * Given an enum constant declaration, returns all possible constructors that match the arity and
   * known types of the arguments.
   *
   * @param enumConstant The enum constant declaration
   * @return All possible constructor declarations
   */
  public static List<ConstructorDeclaration>
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
   * Finds all solvable ancestors, given a type declaration to start.
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
    List<ClassOrInterfaceType> extendedOrImplemented = new ArrayList<>();

    if (start instanceof NodeWithExtends<?> withExtends) {
      extendedOrImplemented.addAll(withExtends.getExtendedTypes());
    }
    if (start instanceof NodeWithImplements<?> withImplements) {
      extendedOrImplemented.addAll(withImplements.getImplementedTypes());
    }

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
   * @param fqn The fully-qualified type name
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
}
