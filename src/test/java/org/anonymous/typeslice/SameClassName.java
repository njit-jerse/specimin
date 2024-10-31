package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if TypeSlice will work properly where there are two classes with the same
 * name
 */
public class SameClassName {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "sameclassname",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#secondCalculator()"});
  }
}
