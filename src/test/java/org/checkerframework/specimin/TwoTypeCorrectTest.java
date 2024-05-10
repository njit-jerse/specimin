package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that Specimin correctly handles multiple subtyping constraints that can only be
 * detected via javac.
 */
public class TwoTypeCorrectTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "twotypecorrect",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Type)"});
  }
}
