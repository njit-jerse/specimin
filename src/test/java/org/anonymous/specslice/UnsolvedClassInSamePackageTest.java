package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if there is an unsolved class not present among import statements,
 * SpecSlice will assume that the class is in the same directory as the current class.
 */
public class UnsolvedClassInSamePackageTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "unsolvedclassinsamepackage",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
