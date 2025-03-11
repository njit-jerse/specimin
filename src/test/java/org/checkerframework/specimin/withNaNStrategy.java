package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that a simple Java file with no dependencies and a single target method with one
 * method that it depends on results in that depended-on method being replaced by an empty body.
 */
public class withNaNStrategy {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "withNaNStrategy",
        new String[] {"com/example/Median.java"},
        new String[] {"com.example.Median#withNaNStrategy(NaNStrategy)"});
  }
}
