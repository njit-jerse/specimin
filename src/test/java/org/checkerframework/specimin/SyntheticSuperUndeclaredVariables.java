package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if Specimin will work properly where there is a super variables call while
 * the parent class file is not in the root directory physically
 */
public class SyntheticSuperUndeclaredVariables {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "syntheticsuperundeclaredvariables",
        new String[] {"com/example/Dog.java"},
        new String[] {"com.example.Dog#isBornFromEggs()"});
  }
}
