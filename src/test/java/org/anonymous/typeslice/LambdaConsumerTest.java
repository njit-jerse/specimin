package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that we infer java.util.function.Consumer when a lambda that takes one parameter
 * but does not return a value is passed to a function in a synthetic class.
 */
public class LambdaConsumerTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "lambdaconsumer",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(LambdaUser)"});
  }
}
