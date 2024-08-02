package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that code that imports compiler components doesn't cause SpecSlice to fail. This
 * is the test for https://github.com/njit-jerse/specSlice/issues/284.
 */
public class JdkCompilerTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "jdkcompiler",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Tree)"});
  }
}
