package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test verifies that when a Java file contains a targeted method that overrides a method in a
 * superclass, the minimized version of the Java file will include the original method from the
 * superclass with an empty body.
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
