package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks for an infinite loop. */
public class Issue272Test {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "issue272",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
