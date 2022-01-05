package org.checkerframework.specimin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.junit.Assert;
import org.junit.Test;

/**
 * This test checks that a simple Java file with no dependencies whatsoever and a single target
 * method is returned unaltered by specimin.
 */
public class NoDependenciesReturnsSameTest {
  @Test
  @SuppressWarnings("all")
  public void runTest() {
    // Create output directory
    Path outputDir = null;
    try {
      outputDir = Files.createTempDirectory("specimin-test-");
    } catch (IOException e) {
      // TODO: what should we do if no temp directory can be created?
      Assert.fail("cannot create temporary directory for test output: " + e);
      return;
    }
    if (outputDir == null) {
      Assert.fail("temporary directory for output was null");
      return;
    }
    // Run specimin on target
    SpeciminRunner.main(
        "--outputDirectory",
        outputDir.toAbsolutePath().toString(),
        "--root",
        Path.of("src/test/resources/nodependenciesreturnsame/input/").toAbsolutePath().toString(),
        "--targetFiles",
        "com/example/Simple.java");

    // Diff the files to ensure that specimin's output is what we expect
    ProcessBuilder builder = new ProcessBuilder();
    boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
    if (isWindows) {
      // TODO: make this work
      Assert.fail("specimin cannot be tested on Windows");
      return;
    } else {
      builder.command(
          "sh",
          "-c",
          "diff",
          "-r",
          outputDir.toAbsolutePath().toString(),
          Path.of("src/test/resources/nodependenciesreturnsame/expected")
              .toAbsolutePath()
              .toString());
    }
    builder.directory(new File(System.getProperty("user.home")));
    Process process = null;
    try {
      process = builder.start();
    } catch (IOException e) {
      Assert.fail("cannot start diff process: " + e);
      return;
    }
    StreamGobbler streamGobbler = new StreamGobbler(process.getInputStream(), System.out::println);
    Executors.newSingleThreadExecutor().submit(streamGobbler);
    int exitCode = 0;
    try {
      exitCode = process.waitFor();
    } catch (InterruptedException e) {
      Assert.fail("diff process interrupted: " + e);
      return;
    }
    Assert.assertEquals(0, exitCode);
  }

  private static class StreamGobbler implements Runnable {
    private InputStream inputStream;
    private Consumer<String> consumer;

    public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
      this.inputStream = inputStream;
      this.consumer = consumer;
    }

    @Override
    public void run() {
      new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumer);
    }
  }
}
