package org.checkerframework.specimin;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.signature.qual.ClassGetSimpleName;
import org.checkerframework.checker.signature.qual.DotSeparatedIdentifiers;
import org.checkerframework.checker.signature.qual.FullyQualifiedName;

/**
 * The visitor for the preliminary phase of Specimin. This visitor goes through the input files,
 * notices all the methods belonging to classes not in the source codes, and creates synthetic
 * versions of those classes and methods. This preliminary step helps to prevent
 * UnsolvedSymbolException errors for the next phases.
 */
public class UnsolvedSymbolVisitor extends ModifierVisitor<Void> {

  /**
   * The unsolved parent class of this current class file. If there is no unsolved parent class,
   * then the value of this variable is an empty string
   */
  private @ClassGetSimpleName String parentClass = "";

  /** The package of this class */
  private String currentPackage = "";

  /**
   * This map will map the name of variables in the current class and its corresponding declaration
   */
  private Map<String, String> variablesAndDeclaration;

  /**
   * Based on the method declarations in the current class, this map will map the name of the
   * methods with their corresponding return types
   */
  private Map<String, @ClassGetSimpleName String> methodAndReturnType;

  /** List of classes not in the source codes */
  private Set<UnsolvedClass> missingClass;

  /** The same as the root being used in SpeciminRunner */
  private String rootDirectory;

  /** This instance maps the name of a synthetic method with its synthetic class */
  private Map<String, UnsolvedClass> syntheticMethodAndClass;

  /**
   * This is to check if the current synthetic files are enough to prevent UnsolvedSymbolException
   * or we still need more.
   */
  private boolean gotException;

  /**
   * The list of classes that have been created. We use this list to delete all the temporary
   * synthetic classes when Specimin finishes its run
   */
  private Set<Path> createdClass;

  /** List of import statement from the current compilation unit that is being visited */
  private List<String> importStatement;

  /** This map the classes in the compilation unit with the related package */
  private Map<String, String> classAndPackageMap;

  /**
   * If there is any import statement that ends with *, this string will be replaced by one of the
   * class from those import statements.
   */
  private String chosenPackage = "";

  /** This set has fully-qualified class names that come from jar files input */
  private Set<@FullyQualifiedName String> classesFromJar = new HashSet<>();

  /**
   * Create a new UnsolvedSymbolVisitor instance
   *
   * @param rootDirectory the root directory of the input files
   */
  public UnsolvedSymbolVisitor(String rootDirectory) {
    this.rootDirectory = rootDirectory;
    this.missingClass = new HashSet<>();
    this.gotException = true;
    this.importStatement = new ArrayList<>();
    this.classAndPackageMap = new HashMap<>();
    this.createdClass = new HashSet<>();
    this.syntheticMethodAndClass = new HashMap<>();
    this.methodAndReturnType = new HashMap<>();
    this.variablesAndDeclaration = new HashMap<>();
  }

  /**
   * Set importStatement equals to the list of import statements from the current compilation unit.
   * Also update the classAndPackageMap based on this new list.
   *
   * @param listOfImports NodeList of import statements from the compilation unit
   */
  public void setImportStatement(NodeList<ImportDeclaration> listOfImports) {
    List<String> currentImportList = new ArrayList<>();
    for (ImportDeclaration importStatement : listOfImports) {
      String importAsString = importStatement.getNameAsString();
      currentImportList.add(importAsString);
    }
    this.importStatement = currentImportList;
    this.setclassAndPackageMap();
  }

  /**
   * This method sets the value of classesFromJar based on the known class of jar type solvers
   *
   * @param jarPaths
   */
  public void setClassesFromJar(List<String> jarPaths) {
    for (String path : jarPaths) {
      try {
        classesFromJar.addAll(new JarTypeSolver(path).getKnownClasses());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
  /**
   * This method sets the classAndPackageMap. This method is called in the method
   * setImportStatement, as classAndPackageMap and importStatements should always be in sync.
   */
  private void setclassAndPackageMap() {
    for (String importStatement : this.importStatement) {
      String[] importParts = importStatement.split("\\.");
      if (importParts.length > 0) {
        String className = importParts[importParts.length - 1];
        String packageName = importStatement.replace("." + className, "");
        if (className.equals("*")) {
          if (!chosenPackage.equals("")) {
            throw new RuntimeException(
                "Multiple wildcard import statements found. Please use explicit import"
                    + " statements.");
          }
          chosenPackage = packageName;
        } else {
          this.classAndPackageMap.put(className, packageName);
        }
      }
    }
  }

  /**
   * Get the value of parentclass
   *
   * @return parentClass the value of parentClass
   */
  public String getParentClass() {
    return parentClass;
  }

  /**
   * Get the value of gotException
   *
   * @return gotException the value of gotException
   */
  public boolean gettingException() {
    return gotException;
  }

  /**
   * Get the classes that have been created by the current iteration of the visitor.
   *
   * @return createdClass the Set of Path of classes that have beenc created
   */
  public Set<Path> getCreatedClass() {
    return createdClass;
  }

  /**
   * Set gotException to false. This method is to be used at the beginning of each iteration of the
   * visitor.
   */
  public void setExceptionToFalse() {
    gotException = false;
  }

  @Override
  public Visitable visit(PackageDeclaration node, Void arg) {
    this.currentPackage = node.getNameAsString();
    return super.visit(node, arg);
  }

  @Override
  public Visitable visit(ClassOrInterfaceDeclaration node, Void arg) {
    if (node.getExtendedTypes().isNonEmpty()) {
      // note that since Specimin does not have access to the class paths of the project, all the
      // unsolved methods related to inheritance will be placed in the parent class, even if there
      // is a grandparent class and so forth.
      SimpleName parentClassSimpleName = node.getExtendedTypes().get(0).getName();
      parentClass = parentClassSimpleName.asString();
    }
    return super.visit(node, arg);
  }

  @Override
  public Visitable visit(ExplicitConstructorInvocationStmt node, Void arg) {
    try {
      // check if the symbol is solvable. If it is, then there's no need to create a synthetic file.
      node.resolve().getQualifiedSignature();
      return super.visit(node, arg);
    } catch (Exception e) {
      NodeList<Expression> arguments = node.getArguments();
      List<String> parametersList = new ArrayList<>();
      for (Expression parameter : arguments) {
        if (!canBeSolved(parameter)) {
          return super.visit(node, arg);
        }
        ResolvedType type = parameter.calculateResolvedType();
        if (type instanceof PrimitiveType) {
          parametersList.add(type.asPrimitive().name());
        } else if (type instanceof ReferenceType) {
          parametersList.add(type.asReferenceType().getQualifiedName());
        }
      }
      UnsolvedMethod constructorMethod = new UnsolvedMethod(this.parentClass, "", parametersList);
      // if the parent class can not be found in the import statements, Specimin assumes it is in
      // the same package as the child class.
      UnsolvedClass parentClass =
          new UnsolvedClass(
              this.parentClass, classAndPackageMap.getOrDefault(this.parentClass, currentPackage));
      parentClass.addMethod(constructorMethod);
      updateMissingClass(parentClass);
      return super.visit(node, arg);
    }
  }

  @Override
  public Visitable visit(FieldDeclaration node, Void arg) {
    for (VariableDeclarator var : node.getVariables()) {
      String variableName = var.getNameAsString();
      String variableType = node.getElementType().asString();
      Optional<Expression> potentialValue = var.getInitializer();
      String variableDeclaration = variableType + " " + variableName;
      if (!potentialValue.isEmpty()) {
        String variableValue = potentialValue.get().toString();
        variableDeclaration += " = " + variableValue;
      } else {
        variableDeclaration =
            this.setInitialValueForVariableDeclaration(variableType, variableDeclaration);
      }
      variablesAndDeclaration.put(variableName, variableDeclaration);
    }
    return super.visit(node, arg);
  }

  @Override
  public Visitable visit(MethodDeclaration node, Void arg) {
    Type nodeType = node.getType();
    // since this is a return type of a method, it is a dot-separated identifier
    @SuppressWarnings("signature")
    @DotSeparatedIdentifiers String nodeTypeAsString = nodeType.asString();
    @ClassGetSimpleName String nodeTypeSimpleForm = toSimpleName(nodeTypeAsString);
    methodAndReturnType.put(node.getNameAsString(), nodeTypeSimpleForm);
    return super.visit(node, arg);
  }

  @Override
  public Visitable visit(FieldAccessExpr node, Void p) {
    if (isASuperCall(node) && !canBeSolved(node)) {
      updateSyntheticClassForSuperCall(node);
    }
    return super.visit(node, p);
  }

  @Override
  public Visitable visit(MethodCallExpr method, Void p) {
    if (canBeSolved(method) && isFromAJarFile(method)) {
      updateClassesFromJarSourcesForMethodCall(method);
      return super.visit(method, p);
    }
    if (isASuperCall(method) && !canBeSolved(method)) {
      updateSyntheticClassForSuperCall(method);
      return super.visit(method, p);
    }
    // we will wait for the next run to solve this method call
    if (!canSolveParameters(method)) {
      return super.visit(method, p);
    }
    if (unsolvedAndNotSimple(method)) {
      updateClassSetWithNotSimpleMethodCall(method);
    } else if (calledByAnIncompleteSyntheticClass(method)) {
      @ClassGetSimpleName String incompleteClassName = getSyntheticClass(method);
      updateUnsolvedClassWithMethodCall(method, incompleteClassName, "");
    }
    this.gotException =
        calledByAnUnsolvedSymbol(method)
            || calledByAnIncompleteSyntheticClass(method)
            || unsolvedAndNotSimple(method);
    return super.visit(method, p);
  }

  @Override
  public Visitable visit(Parameter parameter, Void p) {
    try {
      parameter.resolve().describeType();
      return super.visit(parameter, p);
    } catch (UnsolvedSymbolException e) {
      String parameterInString = parameter.toString();
      if (isAClassPath(parameterInString)) {
        // parameterInString needs to be a fully-qualified name. As this parameter has a form of
        // class path, we can say that it is a fully-qualified name
        @SuppressWarnings("signature")
        UnsolvedClass newClass = getSimpleSyntheticClassFromFullyQualifiedName(parameterInString);
        updateMissingClass(newClass);
      } else {
        // since it is unsolved, it could not be a primitive type
        @ClassGetSimpleName String className = parameter.getType().asClassOrInterfaceType().getName().asString();
        UnsolvedClass newClass =
            new UnsolvedClass(
                className, classAndPackageMap.getOrDefault(className, this.chosenPackage));
        updateMissingClass(newClass);
      }
    }
    gotException = true;
    return super.visit(parameter, p);
  }

  @Override
  public Visitable visit(ObjectCreationExpr newExpr, Void p) {
    SimpleName typeName = newExpr.getType().getName();
    String type = typeName.asString();
    if (canBeSolved(newExpr)) {
      if (isFromAJarFile(newExpr)) {
        updateClassesFromJarSourcesForObjectCreation(newExpr);
      }
      return super.visit(newExpr, p);
    }
    this.gotException = true;
    try {
      List<String> argumentsCreation = getArgumentsFromObjectCreation(newExpr);
      UnsolvedMethod creationMethod = new UnsolvedMethod(type, "", argumentsCreation);
      UnsolvedClass newClass =
          new UnsolvedClass(type, classAndPackageMap.getOrDefault(type, this.chosenPackage));
      newClass.addMethod(creationMethod);
      this.updateMissingClass(newClass);
    } catch (Exception q) {
      // can not solve the parameters for this object creation in this current run
      return super.visit(newExpr, p);
    }
    return super.visit(newExpr, p);
  }

  /**
   * Given the variable type and the basic declaration of that variable (such as "int x", "boolean
   * y", "Car redTruck",...), this methods will add an initial value to that declaration of the
   * variable. The way the initial value is chosen is based on the document of the Java Language:
   * https://docs.oracle.com/javase/specs/jls/se7/html/jls-4.html#jls-4.12.5
   *
   * @param variableType the type of the variable
   * @param variableDeclaration the basic declaration of that variable
   * @return the declaration of the variable with an initial value
   */
  public static String setInitialValueForVariableDeclaration(
      String variableType, String variableDeclaration) {
    if (variableType.equals("byte")) {
      return variableDeclaration + " = (byte)0";
    } else if (variableType.equals("short")) {
      return variableDeclaration + " = (short)0";
    } else if (variableType.equals("int")) {
      return variableDeclaration + " = 0";
    } else if (variableType.equals("long")) {
      return variableDeclaration + " = 0L";
    } else if (variableType.equals("float")) {
      return variableDeclaration + " = 0.0f";
    } else if (variableType.equals("double")) {
      return variableDeclaration + " = 0.0d";
    } else if (variableType.equals("char")) {
      return variableDeclaration + " = '\\u0000'";
    } else if (variableType.equals("boolean")) {
      return variableDeclaration + " = false";
    } else {
      return variableDeclaration + " = null";
    }
  }

  /**
   * Given a class name that can either be fully-qualified or simple, this method will convert that
   * class name to a simple name.
   *
   * @param className the class name to be converted
   * @return the simple form of that class name
   */
  // We can have certainty that this method is true as the last element of a class name is the
  // simple form of that name
  @SuppressWarnings("signature")
  public static @ClassGetSimpleName String toSimpleName(@DotSeparatedIdentifiers String className) {
    String[] elements = className.split("[.]");
    if (elements.length < 2) {
      return className;
    }
    return elements[elements.length - 1];
  }

  /**
   * This method will add a new method declaration to a synthetic class based on the unsolved method
   * call in the original input. User can choose the desired return type for the added method. The
   * desired return type can be an empty string, and in that case, Specimin will create another
   * synthetic class to be the return type of that method.
   *
   * @param method the method call in the original input
   * @param className the name of the synthetic class
   * @param desiredReturnType the desired return type for this method
   */
  public void updateUnsolvedClassWithMethodCall(
      MethodCallExpr method,
      @ClassGetSimpleName String className,
      @ClassGetSimpleName String desiredReturnType) {
    String methodName = method.getNameAsString();
    String returnType = "";
    if (desiredReturnType.equals("")) {
      returnType = returnNameForMethod(methodName);
    } else {
      returnType = desiredReturnType;
    }
    UnsolvedClass missingClass =
        new UnsolvedClass(
            className, classAndPackageMap.getOrDefault(className, this.chosenPackage));
    UnsolvedMethod thisMethod =
        new UnsolvedMethod(methodName, returnType, getArgumentsFromMethodCall(method));
    missingClass.addMethod(thisMethod);
    syntheticMethodAndClass.put(methodName, missingClass);
    this.updateMissingClass(missingClass);
    if (desiredReturnType.equals("")) {
      UnsolvedClass returnTypeForThisMethod =
          new UnsolvedClass(returnType, missingClass.getPackageName());
      this.updateMissingClass(returnTypeForThisMethod);
      classAndPackageMap.put(
          returnTypeForThisMethod.getClassName(), returnTypeForThisMethod.getPackageName());
    }
  }

  /**
   * Checks if the given expression is solvable because the class file containing its symbol is
   * found in one of the jar files provided via the {@code --jarPath} option.
   *
   * @param expr The expression to be checked for solvability.
   * @return true iff the expression is solvable because its class file was found in one of the jar
   *     files.
   */
  public boolean isFromAJarFile(Expression expr) {
    String className;
    if (expr instanceof MethodCallExpr) {
      className =
          ((MethodCallExpr) expr).resolve().getPackageName()
              + "."
              + ((MethodCallExpr) expr).resolve().getClassName();
    } else if (expr instanceof ObjectCreationExpr) {
      String shortName = ((ObjectCreationExpr) expr).getTypeAsString();
      String packageName = classAndPackageMap.getOrDefault(shortName, this.chosenPackage);
      className = packageName + "." + shortName;
    } else {
      throw new RuntimeException("Unexpected call: " + expr + ". Contact developers!");
    }
    return classesFromJar.contains(className);
  }

  /**
   * This method updates a synthetic file based on a solvable expression. The input expression is
   * solvable because its data is in the jar files that Specimin taks as input.
   *
   * @param expr the expression to be used
   */
  public void updateClassesFromJarSourcesForMethodCall(MethodCallExpr expr) {
    if (!isFromAJarFile(expr)) {
      throw new RuntimeException(
          "Check with isFromAJarFile first before using updateClassesFromJarSources");
    }
    String methodName = expr.getNameAsString();
    ResolvedMethodDeclaration methodSolved = expr.resolve();
    @SuppressWarnings(
        "signature") // this is not a precise assumption, as getClassName() will return a
    // @FullyQualifiedName if the class is not of primitive type. However, this is
    // favorable, since we don't have to write any additional import statements.
    @ClassGetSimpleName String className = methodSolved.getClassName();
    String packageName = methodSolved.getPackageName();
    @SuppressWarnings(
        "signature") // this is not a precise assumption, as getReturnType().describe() will return
    // a @FullyQualifiedName if the class is not of primitive type. However, this
    // is favorable, since we don't have to write any additional import statements.
    @ClassGetSimpleName String returnType = methodSolved.getReturnType().describe();
    List<String> argumentsList = getArgumentsFromMethodCall(expr);
    UnsolvedClass missingClass = new UnsolvedClass(className, packageName);
    UnsolvedMethod thisMethod = new UnsolvedMethod(methodName, returnType, argumentsList);
    missingClass.addMethod(thisMethod);
    syntheticMethodAndClass.put(methodName, missingClass);
    this.updateMissingClass(missingClass);
  }

  /**
   * This method updates a synthetic file based on a solvable expression. The input expression is
   * solvable because its data is in the jar files that Specimin taks as input.
   *
   * @param expr the expression to be used
   */
  public void updateClassesFromJarSourcesForObjectCreation(ObjectCreationExpr expr) {
    if (!isFromAJarFile(expr)) {
      throw new RuntimeException(
          "Check with isFromAJarFile first before using updateClassesFromJarSources");
    }
    String objectName = expr.getType().getName().asString();
    ResolvedReferenceTypeDeclaration objectSolved = expr.resolve().declaringType();
    @SuppressWarnings(
        "signature") // this is not a precise assumption, as getClassName() will return a
    // @FullyQualifiedName if the class is not of primitive type. However, this is
    // favorable, since we don't have to write any additional import statements.
    @ClassGetSimpleName String className = objectSolved.getClassName();
    String packageName = objectSolved.getPackageName();
    List<String> argumentsList = getArgumentsFromObjectCreation(expr);
    UnsolvedClass missingClass = new UnsolvedClass(className, packageName);
    UnsolvedMethod thisMethod = new UnsolvedMethod(objectName, "", argumentsList);
    missingClass.addMethod(thisMethod);
    this.updateMissingClass(missingClass);
  }

  /**
   * This method checks if an expression is called by the super keyword. For example, super.visit()
   * is such an expression.
   *
   * @param node the expression to be checked
   * @return true if method is a super call
   */
  public static boolean isASuperCall(Expression node) {
    if (node instanceof MethodCallExpr) {
      Optional<Expression> caller = node.asMethodCallExpr().getScope();
      if (caller.isEmpty()) {
        return false;
      }
      return caller.get().isSuperExpr();
    } else if (node instanceof FieldAccessExpr) {
      Expression caller = node.asFieldAccessExpr().getScope();
      return caller.isSuperExpr();
    } else {
      throw new RuntimeException("Unforeseen expression: " + node);
    }
  }

  /**
   * For a super call, this method will update the corresponding synthetic class
   *
   * @param expr the super call expression to be taken as input
   */
  public void updateSyntheticClassForSuperCall(Expression expr) {
    if (!isASuperCall(expr)) {
      throw new RuntimeException(
          "Check if isASuperCall returns true before calling updateSyntheticClassForSuperCall");
    }
    if (expr instanceof MethodCallExpr) {
      updateUnsolvedClassWithMethodCall(
          expr.asMethodCallExpr(),
          this.parentClass,
          methodAndReturnType.getOrDefault(expr.asMethodCallExpr().getNameAsString(), ""));
    } else {
      updateUnsolvedClassWithVariables(
          expr.asFieldAccessExpr().getNameAsString(),
          parentClass,
          classAndPackageMap.getOrDefault(parentClass, this.currentPackage));
    }
  }

  /**
   * This method will add a new field declaration to a synthetic class. This class is mainly used
   * for unsolved parent class. The declaration of the field in the parent class will be the same as
   * the declaration in the child class since Specimin does not have access to much information. If
   * the field is not found in the child class, Specimin will create a synthetic class to be the
   * type of that field.
   *
   * @param var the field to be added
   * @param className the name of the synthetic class
   * @param packageName the package of the synthetic class
   */
  public void updateUnsolvedClassWithVariables(
      String var, @ClassGetSimpleName String className, String packageName) {
    UnsolvedClass relatedClass = new UnsolvedClass(className, packageName);
    if (variablesAndDeclaration.containsKey(var)) {
      String variableExpression = variablesAndDeclaration.get(var);
      relatedClass.addFields(variableExpression);
      updateMissingClass(relatedClass);
    } else {
      // since it is just simple string combination, it is a simple name
      @SuppressWarnings("signature")
      @ClassGetSimpleName String variableType = "SyntheticTypeFor" + toCapital(var);
      UnsolvedClass varType = new UnsolvedClass(variableType, packageName);
      String variableExpression =
          String.format("%s %s = new %s();", variableType, var, variableType);
      relatedClass.addFields(variableExpression);
      updateMissingClass(relatedClass);
      updateMissingClass(varType);
    }
  }

  /**
   * This method checks if the current run of UnsolvedSymbolVisitor can solve the parameters' types
   * of a method call
   *
   * @param method the method call to be checked
   * @return true if UnsolvedSymbolVisitor can solve the types of parameters of method
   */
  public static boolean canSolveParameters(MethodCallExpr method) {
    NodeList<Expression> paraList = method.getArguments();
    if (paraList.isEmpty()) {
      return true;
    }
    for (Expression parameter : paraList) {
      if (!canBeSolved(parameter)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Given a method call, this method returns the list of types of the parameters of that method
   *
   * @param method the method to be analyzed
   * @return the types of parameters of method
   */
  public static List<String> getArgumentsFromMethodCall(MethodCallExpr method) {
    List<String> parametersList = new ArrayList<>();
    NodeList<Expression> paraList = method.getArguments();
    for (Expression parameter : paraList) {
      ResolvedType type = parameter.calculateResolvedType();
      // for reference type, we need the fully-qualified name to avoid having to add additional
      // import statements.
      if (type.isReferenceType()) {
        parametersList.add(((ResolvedReferenceType) type).getQualifiedName());
      } else if (type.isPrimitive()) {
        parametersList.add(type.describe());
      }
    }
    return parametersList;
  }

  /**
   * Given a new object creation, this method returns the list of types of the parameters of that
   * call
   *
   * @param creationExpr the object creation call
   * @return the types of parameters of the object creation method
   */
  public static List<String> getArgumentsFromObjectCreation(ObjectCreationExpr creationExpr) {
    List<String> parametersList = new ArrayList<>();
    NodeList<Expression> paraList = creationExpr.getArguments();
    for (Expression parameter : paraList) {
      ResolvedType type = parameter.calculateResolvedType();
      if (type.isReferenceType()) {
        parametersList.add(((ResolvedReferenceType) type).getQualifiedName());
      } else if (type.isPrimitive()) {
        parametersList.add(type.describe());
      }
    }
    return parametersList;
  }

  /**
   * As the name suggests, this method takes a MethodCallExpr instance as the input and checks if
   * the method in that expression is called by an unsolved symbol.
   *
   * @param method the method call to be analyzed
   * @return true if the method involved is called by an unsolved symbol
   */
  public static boolean calledByAnUnsolvedSymbol(MethodCallExpr method) {
    Optional<Expression> caller = method.getScope();
    if (!caller.isPresent()) {
      return false;
    }
    Expression callerExpression = caller.get();
    return !canBeSolved(callerExpression);
  }

  /**
   * This methods check if an Expression instance can be solved by SymbolSolver of JavaParser. If
   * the Expression instance can be solved, there is no need to create any synthetic method or class
   * for it.
   *
   * @param expr the expression to be checked
   * @return true if the expression can be solved
   */
  public static boolean canBeSolved(Expression expr) {
    try {
      expr.calculateResolvedType();
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * This method takes a MethodCallExpr as an instance, and check if the method involved is called
   * by an incomplete synthetic class. It should be noted that an incomplete synthetic class is
   * different from a non-existing synthetic class. In this context, an incomplete synthetic class
   * is a compilable class but missing some methods.
   *
   * @param method a MethodCallExpr instance
   * @return true if the method involved is called by an incomplete synthetic class
   */
  public static boolean calledByAnIncompleteSyntheticClass(MethodCallExpr method) {
    if (calledByAnUnsolvedSymbol(method)) {
      return false;
    }
    try {
      method.calculateResolvedType();
      return false;
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * Given a MethodCallExpr instance, this method will return the synthetic class for the method
   * involved. Thus, make sure that the input method actually belongs to an existing synthetic class
   * before calling this method {@link
   * UnsolvedSymbolVisitor#calledByAnIncompleteSyntheticClass(MethodCallExpr)}}
   *
   * @param method the method call to be analyzed
   * @return the name of the synthetic class of that method
   */
  public static @ClassGetSimpleName String getSyntheticClass(MethodCallExpr method) {
    // if calledByAnIncompleteSyntheticClass returns true for this method call, we know that it has
    // a caller.
    ResolvedType callerExpression = method.getScope().get().calculateResolvedType();
    if (callerExpression instanceof ResolvedReferenceType) {
      ResolvedReferenceType referCaller = (ResolvedReferenceType) callerExpression;
      @FullyQualifiedName String callerName = referCaller.getQualifiedName();
      @ClassGetSimpleName String callerSimple = fullyQualifiedToSimple(callerName);
      return callerSimple;
    } else {
      throw new RuntimeException("Unexpected expression: " + callerExpression);
    }
  }

  /**
   * This method converts a @FullyQualifiedName classname to a @ClassGetSimpleName classname. Note
   * that there is warning suppression here. It is safe to claim that if we split
   * a @FullyQualifiedName name by dot ([.]), then the last part is the @ClassGetSimpleName part.
   *
   * @param fullyQualifiedName a @FullyQualifiedName classname
   * @return the @ClassGetSimpleName version of that class
   */
  public static @ClassGetSimpleName String fullyQualifiedToSimple(
      @FullyQualifiedName String fullyQualifiedName) {
    @SuppressWarnings("signature")
    @ClassGetSimpleName String simpleName = fullyQualifiedName.substring(fullyQualifiedName.lastIndexOf(".") + 1);
    return simpleName;
  }

  /**
   * Given the name of an unsolved method, this method will return the name of the synthetic return
   * type for that method. The name is in @ClassGetSimpleName form
   *
   * @param methodName the name of a method
   * @return that name in @ClassGetSimpleName form
   */
  public static @ClassGetSimpleName String returnNameForMethod(String methodName) {
    String capitalizedMethodName =
        methodName.substring(0, 1).toUpperCase() + methodName.substring(1);
    @SuppressWarnings("signature")
    @ClassGetSimpleName String returnName = capitalizedMethodName + "ReturnType";
    return returnName;
  }

  /**
   * This method is to update the missingClass list. The reason we have this update is to add a
   * method to an existing class.
   *
   * @param missedClass the class to be updated
   */
  public void updateMissingClass(UnsolvedClass missedClass) {
    Iterator<UnsolvedClass> iterator = missingClass.iterator();
    while (iterator.hasNext()) {
      UnsolvedClass e = iterator.next();
      if (e.getClassName().equals(missedClass.getClassName())) {
        for (UnsolvedMethod method : missedClass.getMethods()) {
          if (!method.getReturnType().equals("")) {
            e.addMethod(method);
          }
        }
        for (String variablesDescription : missedClass.getClassFields()) {
          e.addFields(variablesDescription);
        }
        return;
      }
    }
    missingClass.add(missedClass);
  }

  /**
   * The method to update synthetic files. After each run, we might have new synthetic files to be
   * created, or new methods to be added to existing synthetic classes. This method will delete all
   * the synthetic files from the previous run and re-create those files with the input from the
   * current run.
   */
  public void updateSyntheticSourceCode() {
    for (UnsolvedClass missedClass : missingClass) {
      this.deleteOldSyntheticClass(missedClass);
      this.createMissingClass(missedClass);
    }
  }

  /**
   * This method is to delete a synthetic class. If that synthetic class is not created yet, the
   * method will do nothing.
   *
   * @param missedClass a synthetic class to be deleted
   */
  public void deleteOldSyntheticClass(UnsolvedClass missedClass) {
    String classPackage =
        classAndPackageMap.getOrDefault(missedClass.getClassName(), this.chosenPackage);
    String filePathStr =
        this.rootDirectory + classPackage + "/" + missedClass.getClassName() + ".java";
    Path filePath = Path.of(filePathStr);
    try {
      Files.delete(filePath);
    } catch (IOException e) {
      // It means that the class that has not been created in the previous run of this visitor
    }
  }

  /**
   * This method create a synthetic file for a class that is not in the source codes. The class will
   * be created in the root directory of the input. All these synthetic files will be deleted when
   * Specimin finishes its run.
   *
   * @param missedClass the class to be added
   */
  public void createMissingClass(UnsolvedClass missedClass) {
    StringBuilder fileContent = new StringBuilder();
    fileContent.append(missedClass);
    String classPackage = missedClass.getPackageName();
    String classDirectory = classPackage.replace(".", "/");
    String filePathStr =
        this.rootDirectory + classDirectory + "/" + missedClass.getClassName() + ".java";
    Path filePath = Paths.get(filePathStr);
    createdClass.add(filePath);
    try {
      Path parentPath = filePath.getParent();
      if (parentPath != null && !Files.exists(parentPath)) {
        Files.createDirectories(parentPath);
      }
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
        writer.write(fileContent.toString());
      } catch (Exception e) {
        throw new Error(e.getMessage());
      }
    } catch (IOException e) {
      System.out.println("Error creating Java file: " + e.getMessage());
    }
  }

  /**
   * This method capitalizes a string. For example, "hello" will become "Hello".
   *
   * @param string the string to be capitalized
   * @return the capitalized version of the string
   */
  public static String toCapital(String string) {
    return string.substring(0, 1).toUpperCase() + string.substring(1);
  }

  /**
   * This method checks if a string is capitalized
   *
   * @param string the string to be checked
   * @return true if the string is capitalized
   */
  public static boolean isCapital(String string) {
    Character first = string.charAt(0);
    return Character.isUpperCase(first);
  }

  /**
   * This method checks if a string has the form of a class path.
   *
   * @param potentialClassPath the string to be checked
   * @return true if the string is a class path
   */
  public static boolean isAClassPath(String potentialClassPath) {
    String[] elements = potentialClassPath.split(".");
    int elementsCount = elements.length;
    return elementsCount > 1 && isCapital(elements[elementsCount - 1]);
  }

  /**
   * Given the name of a class in the @FullyQualifiedName, this method will create a synthetic class
   * for that class
   *
   * @param fullyName the fully-qualified name of the class
   * @return the corresponding instance of UnsolvedClass
   */
  public static UnsolvedClass getSimpleSyntheticClassFromFullyQualifiedName(
      @FullyQualifiedName String fullyName) {
    if (!isAClassPath(fullyName)) {
      throw new RuntimeException(
          "Check with isAClassPath first before using"
              + " getSimpleSyntheticClassFromFullyQualifiedName");
    }
    String className = fullyQualifiedToSimple(fullyName);
    String packageName = fullyName.replace("." + className, "");
    return new UnsolvedClass(className, packageName);
  }

  /**
   * This method checks if a method call is not-simple and unsolved. In this context, we declare a
   * not-simple method call as a method that is directly called by a qualified class name. For
   * example, for this call org.package.Class.methodFirst().methodSecond(),
   * "org.package.Class.methodFirst()" is a not-simple method call, but
   * "org.package.Class.methodFirst().methodSecond()" is a simple one.
   *
   * @param method the method call to be checked
   * @return true if the method call is not simple and unsolved
   */
  public static boolean unsolvedAndNotSimple(MethodCallExpr method) {
    try {
      method.resolve().getReturnType();
      return false;
    } catch (Exception e) {
      Optional<Expression> callerExpression = method.getScope();
      if (callerExpression.isEmpty()) {
        return false;
      }
      String callerToString = callerExpression.get().toString();
      return isAClassPath(callerToString);
    }
  }

  /**
   * For a method call that is not simple, this method will take that method as input and create
   * corresponding synthetic class
   *
   * @param method the method call to be taken as input
   */
  public void updateClassSetWithNotSimpleMethodCall(MethodCallExpr method) {
    String methodCall = method.toString();
    String methodCallWithoutParen = methodCall.replace("()", "");
    String[] methodParts = methodCallWithoutParen.split("[.]");
    int lengthMethodParts = methodParts.length;
    if (lengthMethodParts <= 2) {
      throw new RuntimeException(
          "Need to check the method call with unsolvedAndNotSimple before using"
              + " updateClassSetWithNotSimpleMethodCall");
    }
    String returnTypeClassName = methodParts[0];
    String packageName = methodParts[0];
    String methodName = methodParts[lengthMethodParts - 1];
    for (int i = 1; i < lengthMethodParts - 1; i++) {
      returnTypeClassName = returnTypeClassName + toCapital(methodParts[i]);
      packageName = packageName + "." + methodParts[i];
    }
    returnTypeClassName = returnTypeClassName + toCapital(methodName) + "ReturnType";
    // if the method call is org.package.Class.method(), then the return type of this method will be
    // orgPackageClassMethodReturnType, which is a @ClassGetSimpleName
    @SuppressWarnings("signature")
    @ClassGetSimpleName String thisReturnType = returnTypeClassName;
    UnsolvedClass newClass = new UnsolvedClass(thisReturnType, packageName);
    UnsolvedMethod newMethod =
        new UnsolvedMethod(methodName, thisReturnType, getArgumentsFromMethodCall(method));
    newClass.addMethod(newMethod);
    syntheticMethodAndClass.put(newMethod.toString(), newClass);
    this.updateMissingClass(newClass);
  }

  /**
   * Based on the Map returned by JavaTypeCorrect, this method updates the types of methods in
   * synthetic classes.
   *
   * @param typeToCorrect the Map to be analyzed
   */
  public void updateTypes(
      Map<@ClassGetSimpleName String, @ClassGetSimpleName String> typeToCorrect) {
    for (String incorrectType : typeToCorrect.keySet()) {
      // convert MethodNameReturnType to methodName
      String involvedMethod =
          incorrectType.substring(0, 1).toLowerCase()
              + incorrectType.substring(1).replace("ReturnType", "");
      UnsolvedClass relatedClass = syntheticMethodAndClass.get(involvedMethod);
      if (relatedClass != null) {
        relatedClass.updateMethodByReturnType(incorrectType, typeToCorrect.get(incorrectType));
        this.deleteOldSyntheticClass(relatedClass);
        this.createMissingClass(relatedClass);
      }
    }
  }
}
