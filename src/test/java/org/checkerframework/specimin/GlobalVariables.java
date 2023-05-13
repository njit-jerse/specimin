package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks that a simple Java file with no dependencies whatsoever and a single target
 * method is returned unaltered by specimin.
 */
public class GlobalVariables {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTest(
        "globalvariables",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"});
  }
}
