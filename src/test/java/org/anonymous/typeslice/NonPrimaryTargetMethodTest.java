package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice can handle target methods in non-primary classes. */
public class NonPrimaryTargetMethodTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "nonprimarytargetmethod",
        new String[] {"com/example/Baz.java"},
        new String[] {"com.example.NonPrimary#printMessage()"});
  }
}
