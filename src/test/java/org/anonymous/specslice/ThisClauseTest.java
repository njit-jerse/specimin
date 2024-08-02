package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if SpecSlice can handle the "this" expression. */
public class ThisClauseTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "thisclause",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple()"});
  }
}
