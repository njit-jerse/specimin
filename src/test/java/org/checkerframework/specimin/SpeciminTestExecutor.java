package org.checkerframework.specimin;

import com.google.common.base.Ascii;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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

    // Diff the files to ensure that specimin's output is what we expect
    ProcessBuilder builder = new ProcessBuilder();
    boolean isWindows = Ascii.toLowerCase(System.getProperty("os.name")).startsWith("windows");
    if (isWindows) {
      builder.command(
          "check_differences/check_differences.bat",
          "\"" + outputDir.toAbsolutePath().toString().replace('\\', '/') + "\"",
          "\""
              + Path.of("src/test/resources/" + testName + "/expected")
                  .toAbsolutePath()
                  .toString()
                  .replace('\\', '/')
              + "\"");
      builder.directory(new File(Path.of(".").toAbsolutePath().toString()));
    } else {
      builder.command(
          "diff",
          "-q",
          "-r",
          "-w",
          "-B",
          outputDir.toAbsolutePath().toString(),
          Path.of("src/test/resources/" + testName + "/expected").toAbsolutePath().toString());
      builder.directory(new File(System.getProperty("user.home")));
    }
    Process process;
    try {
      process = builder.start();
    } catch (IOException e) {
      Assert.fail("cannot start diff process: " + e);
      return;
    }
    StringBuilder processOutput = new StringBuilder();
    StreamGobbler streamGobbler =
        new StreamGobbler(process.getInputStream(), processOutput::append);
    Future<?> unused_result = Executors.newSingleThreadExecutor().submit(streamGobbler);
    int exitCode;
    try {
      exitCode = process.waitFor();
    } catch (InterruptedException e) {
      Assert.fail("diff process interrupted: " + e);
      return;
    }
    Assert.assertEquals(
        "Diff failed with the following output: "
            + processOutput
            + "\n Output directory: "
            + outputDir
            + "\n diff command: "
            + builder.command().stream()
                .filter(s -> !"-q".equals(s))
                .collect(Collectors.joining(" "))
            + "\n Error codes: ",
        0,
        exitCode);
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

  /** Code borrowed from https://www.baeldung.com/run-shell-command-in-java. */
  private static class StreamGobbler implements Runnable {
    private InputStream inputStream;
    private Consumer<String> consumer;

    public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
      this.inputStream = inputStream;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
          .lines()
          .forEach(consumer);
    }
  }
}
