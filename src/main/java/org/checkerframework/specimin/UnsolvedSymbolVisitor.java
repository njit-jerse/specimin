package org.checkerframework.specimin;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
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
import java.util.Set;

/**
 * The visitor for the preliminary phase of Specimin. This visitor goes through the input files,
 * spots out all the methods belonging to classes not in the source codes, and creates synthetic
 * versions of those classes and methods. This preliminary step helps to prevent
 * UnsolvedSymbolException errors for the next phases.
 */
public class UnsolvedSymbolVisitor extends ModifierVisitor<Void> {

  /** List of classes not in the source codes */
  private List<UnsolvedClass> missingClass;

  /** The same as the root being used in SpeciminRunner */
  private String rootDirectory;

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
  private Map<String, String> classAndImportMap;

  /**
   * If there is any import statement that ends with *, this string will be replaced by one of the
   * class from those import statements.
   */
  private String chosenPackage = "To.Be.Replaced";

  /**
   * Create a new UnsolvedSymbolVisitor instance
   *
   * @param rootDirectory the root directory of the input files
   */
  public UnsolvedSymbolVisitor(String rootDirectory) {
    this.rootDirectory = rootDirectory;
    this.missingClass = new ArrayList<>();
    this.gotException = true;
    this.importStatement = new ArrayList<>();
    this.classAndImportMap = new HashMap<>();
    this.createdClass = new HashSet<>();
  }

  /**
   * Set importStatement equals to the list of import statements from the current compilation unit.
   * Also update the classAndImportMap based on this new list.
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
    this.setClassAndImportMap();
  }

  /**
   * This method sets the classAndImportMap. This method is called in the method setImportStatement,
   * as classAndImportMap and importStatements should always be in sync.
   */
  private void setClassAndImportMap() {
    for (String importStatement : this.importStatement) {
      String[] importParts = importStatement.split("\\.");
      if (importParts.length > 0) {
        String className = importParts[importParts.length - 1];
        String packageName = importStatement.replace("." + className, "");
        if (className.equals("*")) {
          chosenPackage = packageName;
        } else {
          this.classAndImportMap.put(className, packageName);
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
    String testString = method.getNameAsString();
    try {
      // this line is merely to check if there is UnsolvedSymbolException
      testString = method.resolve().getClassName();
    } catch (UnsolvedSymbolException e) {
      UnsolvedClass missedClass = getTheMissingClass(e);
      // NULL means that our exception-messages-based method is not good enough to find them
      if (!missedClass.getClassName().equals("NULL")) {
        String methodName = method.getNameAsString();
        // for now, we will have the return type of a missing method the same as the class that
        // method belongs to
        UnsolvedMethod missedMethod = new UnsolvedMethod(methodName, missedClass.getClassName());
        missedClass.addMethod(missedMethod);
        this.updateMissingClass(missedClass);
      }
    }
    // there are more elegant ways to update gotException, but the compiler will throw an error if
    // the try block doesn't have any use
    this.gotException = testString.equals(method.getNameAsString());
    return super.visit(method, p);
  }

  @Override
  public Visitable visit(Parameter parameter, Void p) {
    String type = parameter.getNameAsString();
    try {
      type = parameter.resolve().toString();
    } catch (UnsolvedSymbolException e) {
      String className = parameter.getNameAsString();
      UnsolvedClass newClass =
          new UnsolvedClass(
              className, classAndImportMap.getOrDefault(className, this.chosenPackage));
      missingClass.add(newClass);
    }
    // there are more elegant ways to update gotException, but the compiler will throw an error if
    // the try block doesn't have any use
    gotException = type.equals(parameter.getNameAsString());
    return super.visit(parameter, p);
  }

  @Override
  public Visitable visit(ObjectCreationExpr newExpr, Void p) {
    String type = newExpr.getTypeAsString();
    try {
      type = newExpr.resolve().getQualifiedName();
    } catch (UnsolvedSymbolException e) {
      UnsolvedClass newClass =
          new UnsolvedClass(type, classAndImportMap.getOrDefault(type, this.chosenPackage));
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
          e.addMethod(method);
        }
        return;
      }
    }
    missingClass.add(missedClass);
  }

  /**
   * Based on the exception thrown by JavaParser, this method figure out which class file is missing
   * in the source codes. This method is temporary and will be replaced by a proper SymbolSolver in
   * the future.
   *
   * @param exceptionMessage the exception to be analyzed
   * @return an instance of UnsolvedClass correlating to the exception messgae
   */
  public UnsolvedClass getTheMissingClass(UnsolvedSymbolException exceptionMessage) {
    // a null cause means that the exception could not tell us which class file is missing
    if (exceptionMessage.getCause() == null) {
      return new UnsolvedClass("NULL", "NULL");
    }
    Throwable cause = exceptionMessage.getCause();
    // This is a bit hard-coding since the Throwable instance of UnsolvedSymbolException has
    // everything in the form of a message
    if (cause != null && cause.getMessage() != null) {
      String className = cause.getMessage().replace("Unsolved symbol : ", "");
      return new UnsolvedClass(
          className, this.classAndImportMap.getOrDefault(className, this.chosenPackage));
    }
    return new UnsolvedClass("NULL", "NULL");
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
        classAndImportMap.getOrDefault(missedClass.getClassName(), this.chosenPackage);
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
   * Specimin finishes its run. (TO DO: What if Specimin doesn't have the write permission?)
   *
   * @param missedClass the class to be added
   */
  public void createMissingClass(UnsolvedClass missedClass) {
    StringBuilder fileContent = new StringBuilder();
    fileContent.append(missedClass);
    String classPackage =
        classAndImportMap.getOrDefault(missedClass.getClassName(), this.chosenPackage);
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
