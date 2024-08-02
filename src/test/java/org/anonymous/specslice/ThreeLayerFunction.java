package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test verifies the behavior of SpecSlice when dealing with a simple Java file containing a
 * targeted method that calls another method, which in turn calls yet another method (forming a
 * three-layer method call chain). The test check if SpecSlice returns the minimized version of the
 * Java file, where only the targeted method and the nearest method in the call chain included.
 */
public class ThreeLayerFunction {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "threelayerfunctioncall",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"});
  }
}
