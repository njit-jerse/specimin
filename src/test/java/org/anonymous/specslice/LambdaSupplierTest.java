package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that we infer java.util.function.Supplier when a lambda that takes no parameters
 * but does return a value is passed to a function in a synthetic class.
 */
public class LambdaSupplierTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "lambdasupplier",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(LambdaUser)"});
  }
}
