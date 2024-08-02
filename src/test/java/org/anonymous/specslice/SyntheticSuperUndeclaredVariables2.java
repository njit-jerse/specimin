package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if SpecSlice will work properly where there is a super variables call while
 * the parent class file is not in the root directory physically and the field is not declared in
 * the current class. This variant includes two superclass fields to expose a bug in development
 * that caused multiple superclass fields to be converted to the same type, even if they should have
 * been different types.
 */
public class SyntheticSuperUndeclaredVariables2 {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "syntheticsuperundeclaredvariables2",
        new String[] {"com/example/Dog.java"},
        new String[] {"com.example.Dog#isBornFromEggs()"});
  }
}
