package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that when running with the NullAway modularity model, fields are preserved if
 * the target "method" is a constructor.
 */
public class NullAwayCtorTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runNullAwayTestWithoutJarPaths(
        "nullaway-ctors",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple()"});
  }
}
