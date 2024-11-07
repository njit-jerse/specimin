package org.checkerframework.specimin;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.Test;

/**
 * This test is the test described in <a
 * href="https://github.com/njit-jerse/specimin/issues/368">...</a>. The issue caused the original
 * source file A.java to be deleted, and in addition not appear in the output.
 */
public class Issue368Test {
  @Test
  public void runTest() throws IOException {
    Path fileASource = Path.of("src/test/resources/issue368/A_code.java");
    Path fileAPath = Path.of("src/test/resources/issue368/input/com/example/A.java");
    Files.copy(fileASource, fileAPath, StandardCopyOption.REPLACE_EXISTING);
    String fileACode = Files.readString(fileAPath);
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue368", new String[] {"com/example/B.java"}, new String[] {"com.example.B#test()"});

    try {
      assertTrue(Files.exists(fileAPath));
    } finally {
      Files.writeString(fileAPath, fileACode);
    }
  }
}
