package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks that Specimin can successfully target a constructor. */
public class CtorTargetTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "ctortarget",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple(Foo, int)"});
  }
}
