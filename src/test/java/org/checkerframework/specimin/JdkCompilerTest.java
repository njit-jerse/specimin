package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that code that imports compiler components doesn't cause Specimin to fail. This
 * is the test for https://github.com/njit-jerse/specimin/issues/284.
 */
public class JdkCompilerTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "jdkcompiler",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Tree)"});
  }
}
