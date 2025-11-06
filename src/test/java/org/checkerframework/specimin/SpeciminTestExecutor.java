package org.checkerframework.specimin;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.EqualsVisitor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Assert;

/** Utility class containing routines to run Specimin's tests. */
public class SpeciminTestExecutor {

  private SpeciminTestExecutor() {
    throw new UnsupportedOperationException("cannot instatiate this class");
  }

  /**
   * Executes a specimin test. Tests are stored under src/test/resources. Each test folder should
   * contain two folders: "input" and "expected". The "input" folder should be the source root of
   * the input Java program. The "expected" folder should be the source root of the expected output
   * Java program. A test run via this routine runs specimin on the program in the "input" folder of
   * the given test and then compares the output to the program in the "expected" folder using the
   * Unix diff program.
   *
   * <p>The expected way to use this routine is to create a JUnit test method using the {@link
   * org.junit.Test} annotation that contains a single call to this method.
   *
   * @param testName the name of the test folder
   * @param targetFiles the targeted files
   * @param targetMembers the targeted methods or fields, each in the format
   *     class.fully.qualified.Name#methodName(Param1Type, Param2Type, ...) for method and
   *     class.fully.qualified.Name#fieldName for field.
   * @param modularityModel the model to use
   * @param jarPaths the path of jar files for Specimin to solve symbols
   * @throws IOException if some operation fails
   */
  public static void runTest(
      String testName,
      String[] targetFiles,
      String[] targetMembers,
      String modularityModel,
      String[] jarPaths)
      throws IOException {
    // Create output directory
    Path outputDir = null;
    try {
      outputDir = Files.createTempDirectory("specimin-test-");
    } catch (IOException e) {
      Assert.fail("cannot create temporary directory for test output: " + e);
      return;
    }
    if (outputDir == null) {
      Assert.fail("temporary directory for output was null");
      return;
    }

    // Construct the list of arguments.
    List<String> speciminArgs = new ArrayList<>();

    speciminArgs.add("--outputDirectory");
    speciminArgs.add(outputDir.toAbsolutePath().toString());
    speciminArgs.add("--root");
    speciminArgs.add(
        Path.of("src/test/resources/" + testName + "/input/").toAbsolutePath().toString() + "/");
    for (String targetFile : targetFiles) {
      speciminArgs.add("--targetFile");
      speciminArgs.add(targetFile);
    }
    for (String targetMember : targetMembers) {
      if (targetMember.contains("(")) {
        speciminArgs.add("--targetMethod");
        speciminArgs.add(targetMember);
      } else {
        speciminArgs.add("--targetField");
        speciminArgs.add(targetMember);
      }
    }
    speciminArgs.add("--modularityModel");
    speciminArgs.add(modularityModel);
    for (String jarPath : jarPaths) {
      speciminArgs.add("--jarPath");
      speciminArgs.add(jarPath);
    }

    // Run specimin on target
    SpeciminRunner.main(speciminArgs.toArray(new String[0]));

    Path expectedDir = Path.of("src/test/resources/" + testName + "/expected/");
    assertDirectoriesEqual(expectedDir, outputDir);
  }

  /**
   * Compares two directories recursively, parsing all Java files and comparing their ASTs.
   *
   * @param expectedDir the directory with the expected output
   * @param actualDir the directory with the actual output
   * @throws IOException if there is an issue reading the files
   */
  private static void assertDirectoriesEqual(Path expectedDir, Path actualDir) throws IOException {
    try (Stream<Path> expectedStream = Files.walk(expectedDir);
        Stream<Path> actualStream = Files.walk(actualDir)) {
      List<Path> expectedJavaFiles =
          expectedStream
              .filter(p -> p.toString().endsWith(".java"))
              .map(expectedDir::relativize)
              .sorted()
              .toList();

      List<Path> actualJavaFiles =
          actualStream
              .filter(p -> p.toString().endsWith(".java"))
              .map(actualDir::relativize)
              .sorted()
              .toList();

      if (!expectedJavaFiles.equals(actualJavaFiles)) {
        Assert.fail(
            "The set of Java files in the expected and actual directories do not match.\nExpected: "
                + expectedJavaFiles.toString().replace('\\', '/')
                + "\nActual: "
                + actualJavaFiles.toString().replace('\\', '/'));
      }

      for (Path relativePath : expectedJavaFiles) {
        Path expectedFile = expectedDir.resolve(relativePath);
        Path actualFile = actualDir.resolve(relativePath);
        try {
          CompilationUnit expectedCu = StaticJavaParser.parse(expectedFile);
          CompilationUnit actualCu = StaticJavaParser.parse(actualFile);
          if (!EqualsVisitor.equals(actualCu, expectedCu)) {
            Assert.fail(
                "ASTs do not match for file: "
                    + relativePath.toString().replace('\\', '/')
                    + "\nExpected:\n"
                    + expectedCu
                    + "\nActual:\n"
                    + actualCu);
          }
        } catch (Exception e) {
          Assert.fail("Error parsing and comparing files: " + relativePath + "\n" + e);
        }
      }
    }
  }

  /**
   * This method call the method runTest without an array of jar paths. This runs using the CF
   * modularity model.
   *
   * @param testName the name of the test folder
   * @param targetFiles the targeted files
   * @param targetMembers the targeted methods or fields, each in the format
   *     class.fully.qualified.Name#methodName(Param1Type, Param2Type, ...) for method and
   *     class.fully.qualified.Name#fieldName for field.
   * @throws IOException if some operation fails
   */
  public static void runTestWithoutJarPaths(
      String testName, String[] targetFiles, String[] targetMembers) throws IOException {
    runTest(testName, targetFiles, targetMembers, "cf", new String[] {});
  }

  /**
   * This method call the method runTest without an array of jar paths. Runs with the NullAway
   * modularity model.
   *
   * @param testName the name of the test folder
   * @param targetFiles the targeted files
   * @param targetMembers the targeted methods or fields, each in the format
   *     class.fully.qualified.Name#methodName(Param1Type, Param2Type, ...) for method and
   *     class.fully.qualified.Name#fieldName for field.
   * @throws IOException if some operation fails
   */
  public static void runNullAwayTestWithoutJarPaths(
      String testName, String[] targetFiles, String[] targetMembers) throws IOException {
    runTest(testName, targetFiles, targetMembers, "nullaway", new String[] {});
  }
}
