package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks if Specimin can create compilable synthetic files for unsolved method with parameters
 */
public class SyntheticParameters {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTest(
        "syntheticparameters",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#getWarranty(Car)"});
  }
}
