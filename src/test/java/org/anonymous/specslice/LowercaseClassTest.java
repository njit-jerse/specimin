package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that a simple Java file with a lowercase class name (against convention but
 * allowed!) doesn't cause SpecSlice any problems.
 */
public class LowercaseClassTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "lowercaseclass",
        new String[] {"com/example/simple.java"},
        new String[] {"com.example.simple#test()"});
  }
}
