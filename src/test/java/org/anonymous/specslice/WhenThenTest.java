package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that typically-styled test code doesn't cause SpecSlice to lose a method. Based
 * on a bug observed in na-176.
 */
public class WhenThenTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "whenthen",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
