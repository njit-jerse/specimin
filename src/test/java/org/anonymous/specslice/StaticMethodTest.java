package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that SpecSlice makes methods that need to be static, static, even when there is
 * no associated import.
 */
public class StaticMethodTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "staticmethod",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
