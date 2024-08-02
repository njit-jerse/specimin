package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if SpecSlice can handle unsolvable interfaces that happens due to the an input
 * file isolated from its codebase.
 */
public class UnsolvableInterfaceTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "unsolvableinterface",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
