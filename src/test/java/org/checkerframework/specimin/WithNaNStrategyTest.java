package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** TODO */
public class WithNaNStrategyTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "withNaNStrategy",
        new String[] {"com/example/Median.java"},
        new String[] {"com.example.Median#withNaNStrategy(NaNStrategy)"});
  }
}
