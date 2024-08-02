package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if SpecSlice will work properly where there is a super constructor while
 * the parent class file is not in the root directory physically
 */
public class SyntheticSuperConstructor {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "syntheticsuperconstructor",
        new String[] {"com/example/Car.java"},
        new String[] {"com.example.Car#Car()"});
  }
}
