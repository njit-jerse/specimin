package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if SpecSlice will work properly where there is a super variables call while
 * the parent class file is not in the root directory physically and the field is not declared in
 * the current class
 */
public class SyntheticSuperUndeclaredVariables {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "syntheticsuperundeclaredvariables",
        new String[] {"com/example/Dog.java"},
        new String[] {"com.example.Dog#isBornFromEggs()"});
  }
}
