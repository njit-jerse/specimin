package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that a target that mixes static and non-static uses of a class doesn't confuse
 * Specimin.
 */
public class StaticAndNonStaticUseTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "staticandnonstaticuse",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(boolean)"});
  }
}
