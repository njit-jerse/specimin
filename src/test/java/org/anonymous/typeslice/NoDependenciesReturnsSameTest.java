package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that a simple Java file with no dependencies whatsoever and a single target
 * method is returned unaltered by specSlice.
 */
public class NoDependenciesReturnsSameTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "nodependenciesreturnssame",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"});
  }
}
