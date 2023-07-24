package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if Specimin will work properly where there is a super constructor while the
 * parent class file is not in the root directory physically
 */
public class SyntheticSuperConstructor {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTest(
        "syntheticsuperconstructor",
        new String[] {"com/example/Car.java"},
        new String[] {"com.example.Car#Car()"},
        new String[] {});
  }
}
