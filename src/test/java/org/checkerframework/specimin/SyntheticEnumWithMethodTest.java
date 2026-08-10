package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a synthetic enum that has both constants and methods is printed with a
 * semicolon terminating its constant list, as the JLS requires whenever an enum body contains
 * member declarations. This is a test for https://github.com/njit-jerse/specimin/issues/489.
 */
public class SyntheticEnumWithMethodTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "syntheticenumwithmethod",
        new String[] {"org/example/Target.java"},
        new String[] {"org.example.Target#target()"});
  }
}
