package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks for todo */
public class Issue298Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue298",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#isSideEffectFree(ExecutableElement)"});
  }
}
