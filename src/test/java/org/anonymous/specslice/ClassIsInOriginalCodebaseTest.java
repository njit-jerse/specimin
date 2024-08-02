package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if SpecSlice can recognize classes belonging the original codebase correctly.
 */
public class ClassIsInOriginalCodebaseTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "classinoriginalcodebase",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo(SomeClass)"});
  }
}
