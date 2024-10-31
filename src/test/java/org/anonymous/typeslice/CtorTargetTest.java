package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks that TypeSlice can successfully target a constructor. */
public class CtorTargetTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "ctortarget",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple(Foo, int)"});
  }
}
