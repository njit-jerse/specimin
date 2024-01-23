package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks if Specimin can handle the "this" expression.
 * */
public class ThisClauseTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "thisclause",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple()"});
  }
}
