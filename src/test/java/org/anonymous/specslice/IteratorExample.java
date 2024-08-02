package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks for a crash that we found while minimizing some JDK code. */
public class IteratorExample {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "iteratorexample",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
