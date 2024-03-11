package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that we create a synthetic 4-typevar top type for functions that take 3
 * parameters and return a value when a lambda that takes three parameters and returns a value is
 * passed to a function in a synthetic class.
 */
public class LambdaTriFunctionTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdatrifunction",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(LambdaUser)"});
  }
}
