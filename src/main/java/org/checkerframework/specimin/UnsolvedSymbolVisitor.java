package org.checkerframework.specimin;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
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

/**
 * The visitor for the preliminary phase of Specimin. This visitor goes through the input files,
 * notices all the methods belonging to classes not in the source codes, and creates synthetic
 * versions of those classes and methods. This preliminary step helps to prevent
 * UnsolvedSymbolException errors for the next phases.
 */
public class UnsolvedSymbolVisitor extends ModifierVisitor<Void> {

  /** List of classes not in the source codes */
  private Set<UnsolvedClass> missingClass;

  /** The same as the root being used in SpeciminRunner */
  private String rootDirectory;

  private Set<String> classToBeReturnType;
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

  /**
   * This map the classes in the compilation unit with the related import statements in that unit
   */
  private Map<String, String> classAndPackageMap;

  /**
   * If there is any import statement that ends with *, this string will be replaced by one of the
   * class from those import statements.
   */
  private String chosenPackage = "";

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
    this.classToBeReturnType = new HashSet<>();
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
  public Visitable visit(MethodCallExpr method, Void p) {
    String methodSimpleName = method.getNameAsString();
    String capitalizedMethodName =
        methodSimpleName.substring(0, 1).toUpperCase() + methodSimpleName.substring(1);
    if (calledByAnIncompleteSyntheticClass(method)) {
      String incompleteClassName = getSyntheticClass(method);
      UnsolvedClass missingClass =
          new UnsolvedClass(
              incompleteClassName,
              classAndPackageMap.getOrDefault(incompleteClassName, this.chosenPackage));
      UnsolvedClass returnTypeForThisMethod =
          new UnsolvedClass(capitalizedMethodName + "ReturnType", missingClass.getPackageName());
      UnsolvedMethod thisMethod =
          new UnsolvedMethod(methodSimpleName, returnTypeForThisMethod.getClassName());
      missingClass.addMethod(thisMethod);
      classAndPackageMap.put(
          returnTypeForThisMethod.getClassName(), returnTypeForThisMethod.getPackageName());
      this.updateMissingClass(missingClass);
      this.updateMissingClass(returnTypeForThisMethod);
      classToBeReturnType.add(returnTypeForThisMethod.getClassName());
    }

    this.gotException =
        calledByAnUnsolvedSymbol(method) || calledByAnIncompleteSyntheticClass(method);
    return super.visit(method, p);
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
    try {
      callerExpression.calculateResolvedType();
      return false;
    } catch (Exception e) {
      return true;
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

  public static String getSyntheticClass(MethodCallExpr method) {
    String fullNameOfTheClass = method.getScope().get().calculateResolvedType().describe();
    String shortNameOfTheClass =
        fullNameOfTheClass.substring(fullNameOfTheClass.lastIndexOf('.') + 1);
    return shortNameOfTheClass;
  }

  @Override
  public Visitable visit(Parameter parameter, Void p) {
    String type = parameter.getNameAsString();
    try {
      parameter.resolve().describeType();
      return super.visit(parameter, p);
    } catch (UnsolvedSymbolException e) {
      String className = parameter.getTypeAsString();
      UnsolvedClass newClass =
          new UnsolvedClass(
              className, classAndPackageMap.getOrDefault(className, this.chosenPackage));
      updateMissingClass(newClass);
    }
    // there are more elegant ways to update gotException, but the compiler will throw an error if
    // the try block doesn't have any use
    gotException = true;
    return super.visit(parameter, p);
  }

  @Override
  public Visitable visit(ObjectCreationExpr newExpr, Void p) {
    String type = newExpr.getTypeAsString();
    try {
      type = newExpr.resolve().getQualifiedName();
    } catch (UnsolvedSymbolException e) {
      UnsolvedClass newClass =
          new UnsolvedClass(type, classAndPackageMap.getOrDefault(type, this.chosenPackage));
      this.updateMissingClass(newClass);
    }
    this.gotException = type.equals(newExpr.getTypeAsString());
    return super.visit(newExpr, p);
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
}
