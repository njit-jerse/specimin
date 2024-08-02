package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if SpecSlice can handle unsolved parameters with array types. */
public class UnsolvedMethodWithArrayParameter {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "unsolvedmethodwitharrayparameter",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(int[][][])"});
  }
}
