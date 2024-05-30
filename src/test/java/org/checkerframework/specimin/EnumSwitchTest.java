package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks for a non-compilation problem that we found when targeting the Checker
 * Framework. The problem occurred when an enum constant was referenced without any qualification,
 * because it was present in the same package as the target and was unambiguous. In that case,
 * Specimin required special logic to treat it differently than a field.
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
