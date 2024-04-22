package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks TODO */
public class CtorInCatchTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "ctorincatch",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(MethodGen)"});
  }
}
