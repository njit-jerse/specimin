package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Test case that forces a synthetic exception to be created that must be unchecked, or the output
 * won't compile.
 */
public class WithNaNStrategyTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "withnanstrategy",
        new String[] {"com/example/Median.java"},
        new String[] {"com.example.Median#withNaNStrategy(NaNStrategy)"});
  }
}
