package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if Specimin can correctly differentiate between an unsolved field of the current
 * class and an implicitly accessed super variable. Both scenarios may appear as unsolved NameExpr
 * instances in JavaParser. This test ensures that Specimin can correctly identify and handle these
 * distinct cases.
 */
public class HiddenSuperVariables {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "hiddenSuperVariables",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#getMyInteger()", "com.example.Simple#getCorrect()"});
  }
}
