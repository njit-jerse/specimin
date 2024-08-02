package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if SpecSlice will work if there is an anonymous class inside the target
 * method
 */
public class AnonymousClass {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "anonymousclass",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#testAnonymous()"});
  }
}
