package org.checkerframework.specimin;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
   * Create a new JavaTypeCorrect instance. The directories of files in fileNameList are relative to
   * SpeciminRunner
   *
   * @param rootDirectory the root directory of the files to correct types
   * @param fileNameList the list of the relative directory of the files to correct types
   */
  public JavaTypeCorrect(String rootDirectory, Set<String> fileNameList) {
    this.fileNameList = fileNameList;
    this.sourcePath = rootDirectory;
    this.typeToChange = new HashMap<>();
  }

  public Map<String, String> getTypeToChange() {
    return typeToChange;
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
    try {
      String command = "javac";
      File file = new File(filePath);
      String[] arguments = {command, "-sourcepath", sourcePath, file.getAbsolutePath()};
      ProcessBuilder processBuilder = new ProcessBuilder(arguments);
      processBuilder.redirectErrorStream(true);
      Process process = processBuilder.start();
      BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.contains("error: incompatible types")) {
          updateTypeToChange(line);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * This method updates typeToChange by relying on the error messages from javac
   *
   * @param errorMessage the error message to be analyzed
   */
  private void updateTypeToChange(String errorMessage) {
    String[] splitErrorMessage = errorMessage.split("\\s+");
    if (splitErrorMessage.length < 7) {
      throw new RuntimeException("Unexpected type error messages: " + errorMessage);
    }
    /* There are two possible forms of error messages here:
     * 1. error: incompatible types: <type1> cannot be converted to <type2>
     * 2. error: incompatible types: found <type1> required <type2>
     */
    if (errorMessage.contains("cannot be converted to")) {
      typeToChange.put(splitErrorMessage[4], splitErrorMessage[splitErrorMessage.length - 1]);
    } else {
      typeToChange.put(splitErrorMessage[5], splitErrorMessage[splitErrorMessage.length - 1]);
    }
  }
}
