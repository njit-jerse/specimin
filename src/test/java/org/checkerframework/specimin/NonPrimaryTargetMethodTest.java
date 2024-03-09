package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin can handle target methods in non-primary classes. */
public class NonPrimaryTargetMethodTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nonprimarytargetmethod",
        new String[] {"com/example/Baz.java"},
        new String[] {"com.example.NonPrimary#printMessage()"});
  }
}
