package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks that SpecSlice can successfully target a constructor. */
public class CtorTargetTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "ctortarget",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple(Foo, int)"});
  }
}
