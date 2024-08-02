package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if SpecSlice can work for tricky, unsolved parameters. */
public class UnsolvedParameter {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "unsolvedparameter",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#printName(FullName<MiddleName<String>, LastName>)"});
  }
}
