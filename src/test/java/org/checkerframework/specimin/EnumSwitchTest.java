package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks for a non-compilation problem that we found when targeting the Checker
 * Framework.
 */
public class EnumSwitchTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "enumswitch",
        new String[] {"com/example/Simple.java", "com/example/Analysis.java"},
        new String[] {"com.example.Simple#bar(Analysis)"});
  }
}
