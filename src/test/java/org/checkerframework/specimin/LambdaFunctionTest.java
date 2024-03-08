package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks that we can infer a type that's reasonable when a lambda is passed to a function
 * in a synthetic class.
 */
public class LambdaFunctionTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
            "lambdafunction",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(LambdaUser)"});
  }
}
