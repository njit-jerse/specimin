package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that we infer java.util.function.BiFunction when a lambda that takes two
 * parameters and returns a value is passed to a function in a synthetic class.
 */
public class LambdaBiFunctionTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "lambdabifunction",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(LambdaUser)"});
  }
}
