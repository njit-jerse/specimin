package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice can handle qualified paths in an extension clause. */
public class ExtendQualifiedPathTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "extendqualifiedpath",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
