package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that TypeSlice correctly handles multiple subtyping constraints that can only be
 * detected via javac.
 */
public class TwoTypeCorrectTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "twotypecorrect",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Type)"});
  }
}
