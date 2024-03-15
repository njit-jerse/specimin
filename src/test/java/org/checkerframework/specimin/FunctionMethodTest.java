package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks that Specimin doesn't crash when encountering a used
 * method that takes a function as a parameter.
 */
public class FunctionMethodTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "functionmethod",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(List<SqlNode>)"});
  }
}
