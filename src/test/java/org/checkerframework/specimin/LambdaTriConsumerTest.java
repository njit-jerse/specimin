package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that we create a synthetic 3-typevar top type for functions that
 * take 3 parameters and don't return a value when a lambda that takes three
 * parameters and doesn't return a value is passed to a function in a synthetic class.
 */
public class LambdaTriConsumerTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdatriconsumer",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(LambdaUser)"});
  }
}
