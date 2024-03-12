package org.checkerframework.specimin;

import com.google.common.base.Splitter;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class uses javac to analyze files. If there are any incompatible type errors in those files,
 * this class can suggest changes to be made to the existing types in the input files to solve all
 * the type errors. Note: This class can only solve type errors where there are mismatch between
 * types. For cases where there is type inference failed or unfound symbol, try to use
 * UnsolvedSymbolVisitor to add the missing files to the input first. {@link UnsolvedSymbolVisitor}
 */
class JavaTypeCorrect {

  /** List of the files to correct the types */
  public Set<String> fileNameList;

  /**
   * The root directory of the files in the fileNameList. If there are more than one root
   * directories involved, consider using more than one instances of JavaTypeCorrect
   */
  public String sourcePath;

  /**
   * This map is for type correcting. The key is the name of the current incorrect type, and the
   * value is the name of the desired correct type.
   */
  private Map<String, String> typeToChange;

  /**
   * A map that associates the file directory with the set of fully qualified names of types used
   * within that file.
   */
  private Map<String, Set<String>> fileAndAssociatedTypes = new HashMap<>();

  /**
   * Synthetic types that need to extend or implement a class/interface. Note that the stored
   * Strings are simple names (because javac's error messages only give simple names), which is safe
   * because the worst thing that might happen is that an extra synthetic class might accidentally
   * extend or implement an unnecessary interface.
   */
  private Map<String, String> extendedTypes = new HashMap<>();

  /**
   * Create a new JavaTypeCorrect instance. The directories of files in fileNameList are relative to
   * rootDirectory, and rootDirectory is an absolute path
   *
   * @param rootDirectory the root directory of the files to correct types
   * @param fileNameList the list of the relative directory of the files to correct types
   */
  public JavaTypeCorrect(
      String rootDirectory,
      Set<String> fileNameList,
      Map<String, Set<String>> fileAndAssociatedTypes) {
    this.fileNameList = fileNameList;
    this.sourcePath = new File(rootDirectory).getAbsolutePath();
    this.typeToChange = new HashMap<>();
    this.fileAndAssociatedTypes = fileAndAssociatedTypes;
  }

  /**
   * Get the value of typeToChange
   *
   * @return the value of typeToChange
   */
  public Map<String, String> getTypeToChange() {
    return typeToChange;
  }

  /**
   * Get the simple names of synthetic classes that should extend or implement a class/interface (it
   * is not known at this point which). Both keys and values are simple names, due to javac
   * limitations.
   *
   * @return the map described above.
   */
  public Map<String, String> getExtendedTypes() {
    return extendedTypes;
  }

  /**
   * This method updates typeToChange by using javac to run all the files in fileNameList and
   * analyzing the error messages returned by javac
   */
  public void correctTypesForAllFiles() {
    for (String fileName : fileNameList) {
      runJavacAndUpdateTypes(fileName);
    }
  }

  /**
   * This method uses javac to run a file and updates typeToChange if that file has any incompatible
   * type error
   *
   * @param filePath the directory of the file to be analyzed
   */
  public void runJavacAndUpdateTypes(String filePath) {
    Path outputDir;
    try {
      outputDir = Files.createTempDirectory("specimin-javatypecorrect");
    } catch (IOException e) {
      throw new RuntimeException("failed to create a temporary directory");
    }

    try {
      String command = "javac";
      // Note: -d to a tempdir is used to avoid generating .class files amongst the user's files
      // when compilation succeeds. -Xmaxerrs 0 is used to print out all error messages.
      String[] arguments = {
        command,
        "-d",
        outputDir.toAbsolutePath().toString(),
        "-sourcepath",
        sourcePath,
        sourcePath + "/" + filePath,
        "-Xmaxerrs",
        "0"
      };
      ProcessBuilder processBuilder = new ProcessBuilder(arguments);
      processBuilder.redirectErrorStream(true);
      Process process = processBuilder.start();
      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
      String line;
      // incorrect constraint type will be updated here. Other types will be updated by
      // updateTypeToChange. The following pairs of two-line errors are recognized:
      // incompatible constraint types: "equality constraints", "lower bounds"
      // bad operand types for binary operator: "first type", "second type"
      String[] firstConstraints = {"equality constraints: ", "first type: "};
      String[] secondConstraints = {"lower bounds: ", "second type: "};
      String firstConstraintType = "";

      lines:
      while ((line = reader.readLine()) != null) {
        if (line.contains("error: incompatible types")) {
          updateTypeToChange(line, filePath);
          continue lines;
        }
        // these type error with constraint types will be in a pair of lines
        for (String firstConstraint : firstConstraints) {
          if (line.contains(firstConstraint)) {
            firstConstraintType = line.replace(firstConstraint, "").trim();
            continue lines;
          }
        }
        for (String secondConstraint : secondConstraints) {
          if (line.contains(secondConstraint)) {
            String secondConstraintType = line.replace(secondConstraint, "").trim();
            if (isSynthetic(firstConstraintType)) {
              changeType(firstConstraintType, secondConstraintType);
            } else if (isSynthetic(secondConstraintType)) {
              changeType(secondConstraintType, firstConstraintType);
            } else {
              throw new RuntimeException(
                  "JavaTypeCorrect found two incompatible types, but neither is "
                      + "synthetic:\n"
                      + "first constraint type: "
                      + firstConstraintType
                      + "\nsecond constraint type: "
                      + secondConstraintType);
            }
            firstConstraintType = "";
            continue lines;
          }
        }
      }
    } catch (IOException e) {
      System.out.println(e);
    }
  }

  /**
   * This method updates typeToChange by relying on the error messages from javac
   *
   * @param errorMessage the error message to be analyzed
   * @param filePath the path of the file where this error happens
   */
  private void updateTypeToChange(String errorMessage, String filePath) {
    List<String> splitErrorMessage = Splitter.onPattern("\\s+").splitToList(errorMessage);
    if (splitErrorMessage.size() < 7) {
      throw new RuntimeException("Unexpected type error messages: " + errorMessage);
    }
    /* There are two possible forms of error messages here:
     * 1. error: incompatible types: <type1> cannot be converted to <type2>
     * 2. error: incompatible types: found <type1> required <type2>
     */
    if (errorMessage.contains("cannot be converted to")) {
      String rhs = splitErrorMessage.get(4);
      String lhs = splitErrorMessage.get(splitErrorMessage.size() - 1);
      if (lhs.equals("Throwable")) {
        extendedTypes.put(rhs, "Throwable");
      } else if (isSynthetic(lhs)) {
        // This situation occurs if we have created a synthetic field
        // (e.g., in a superclass) that has a type that doesn't match the
        // type of the RHS. In this case, the "correct" type is wrong, and
        // the "incorrect" type is the actual type of the RHS.
        changeType(lhs, tryResolveFullyQualifiedType(rhs, filePath));
      } else if (isSynthetic(rhs)) {
        changeType(rhs, tryResolveFullyQualifiedType(lhs, filePath));
      } else {
        // In this case, neither is truly synthetic (both must be used
        // in the target), so make the rhs a subtype of the lhs
        extendedTypes.put(rhs, lhs);
      }
    } else {
      // TODO: what error message triggers this code? Do we have test cases for it?
      String rhs = splitErrorMessage.get(5);
      String lhs = splitErrorMessage.get(splitErrorMessage.size() - 1);
      if (isSynthetic(lhs)) {
        changeType(lhs, tryResolveFullyQualifiedType(rhs, filePath));
      } else if (isSynthetic(rhs)) {
        changeType(rhs, tryResolveFullyQualifiedType(lhs, filePath));
      } else {
        extendedTypes.put(rhs, lhs);
      }
    }
  }

  /**
   * All instances of the synthetic "incorrect type" will be replaced with the "correct type" in the
   * output of Specimin. This method does handle cases where at least two different types need to be
   * matched (i.e., upper bounds), and should always be called rather than updating {@link
   * #typeToChange} directly.
   *
   * @param incorrectType an incorrect synthetic type that is causing a type error
   * @param correctType a correct type that the incorrect type must be a supertype of, based on the
   *     output of javac
   */
  private void changeType(String incorrectType, String correctType) {
    if (typeToChange.containsKey(incorrectType)) {
      String otherCorrectType = typeToChange.get(incorrectType);
      if (!otherCorrectType.equals(correctType)) {
        // we require a LUB: don't do a direct conversion between the types, but
        // instead retain the "incorrect" synthetic type as a mutual top type
        // for the two other "correct" types.
        typeToChange.remove(incorrectType);
        // TODO: what if one of these "correct" types is non-synthetic?
        // Is that possible? What would the consequences be if so?
        extendedTypes.put(correctType, incorrectType);
        extendedTypes.put(otherCorrectType, incorrectType);
        // once we've made this lub correction, we don't want to
        // continue with our main fix strategy
        return;
      }
    }

    typeToChange.put(incorrectType, correctType);
  }

  /**
   * This method tries to get the fully-qualified name of a type based on the simple name of that
   * type and the class file where that type is used.
   *
   * @param type the type to be taken as input
   * @param filePath the path of the file where type is used
   * @return the fully-qualified name of that type if any. Otherwise, return the original expression
   *     of type.
   */
  public String tryResolveFullyQualifiedType(String type, String filePath) {
    // type is already in the fully qualified format
    if (Splitter.onPattern("\\.").splitToList(type).size() > 1) {
      return type;
    }
    String typeVariable = "";
    if (type.contains("<")) {
      typeVariable = type.substring(type.indexOf("<"));
      type = type.substring(0, type.indexOf("<"));
    }
    if (fileAndAssociatedTypes.containsKey(filePath)) {
      Set<String> fullyQualifiedType = fileAndAssociatedTypes.get(filePath);
      for (String typeFullName : fullyQualifiedType) {
        if (typeFullName.substring(typeFullName.lastIndexOf(".") + 1).equals(type)) {
          return typeFullName + typeVariable;
        }
      }
    }
    return type + typeVariable;
  }

  /**
   * returns true iff the given simple type's name matches one of the patterns used by
   * UnsolvedSymbolVisitor when creating synthetic classes
   *
   * @param typename a simple type name
   * @return true if the name can be synthetic
   */
  public static boolean isSynthetic(String typename) {
    return typename.startsWith("SyntheticTypeFor")
        || typename.endsWith("ReturnType")
        || typename.startsWith("SyntheticFunction")
        || typename.startsWith("SyntheticConsumer")
        || typename.endsWith("SyntheticType");
  }
}
