package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice can handle unsolved parameters with array types. */
public class UnsolvedMethodWithArrayParameter {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "unsolvedmethodwitharrayparameter",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(int[][][])"});
  }
}
