package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin can handle unsolved parameters with array types. */
public class UnsolvedMethodWithArrayParameter {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedmethodwitharrayparameter",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(int[])"});
  }
}
