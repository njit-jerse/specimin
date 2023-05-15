package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that a simple Java file with no dependencies whatsoever and a single target
 * method is returned unaltered by specimin.
 */
public class SuperClass {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTest(
        "superclass",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#printMessage()"});
  }
}
