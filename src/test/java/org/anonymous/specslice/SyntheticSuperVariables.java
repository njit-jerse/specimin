package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if SpecSlice will work properly where there is a super variables call while
 * the parent class file is not in the root directory physically
 */
public class SyntheticSuperVariables {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "syntheticsupervariables",
        new String[] {"com/example/Dog.java"},
        new String[] {"com.example.Dog#setUp()"});
  }
}
