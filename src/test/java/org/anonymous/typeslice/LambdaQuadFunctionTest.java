package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that we create a synthetic 5-typevar top type for functions that take 4
 * parameters and return a value when a lambda that takes four parameters and returns a value is
 * passed to a function in a synthetic class.
 */
public class LambdaQuadFunctionTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "lambdaquadfunction",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(LambdaUser)"});
  }
}
