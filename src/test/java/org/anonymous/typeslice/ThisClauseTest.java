package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice can handle the "this" expression. */
public class ThisClauseTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "thisclause",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple()"});
  }
}
