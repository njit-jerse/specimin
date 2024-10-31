package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice will work for Union types */
public class Issue38Test {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "issue38",
        new String[] {"com/example/Issue38.java"},
        new String[] {"com.example.Issue38#test()"});
  }
}
