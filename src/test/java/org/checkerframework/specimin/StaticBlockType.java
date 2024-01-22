package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks if Specimin can remove static blocks
 */
public class StaticBlockType {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "staticblock",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"});
  }
}
