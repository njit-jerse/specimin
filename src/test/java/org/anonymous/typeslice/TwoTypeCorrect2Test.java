package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks that TypeSlice correctly handles multiple subtyping constraints. */
public class TwoTypeCorrect2Test {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "twotypecorrect2",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Type)"});
  }
}
