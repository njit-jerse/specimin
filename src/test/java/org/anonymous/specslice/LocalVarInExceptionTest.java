package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if SpecSlice can work when the parameter of the catch clause is used in the
 * associating block statement.
 */
public class LocalVarInExceptionTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "localvarinexception",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
