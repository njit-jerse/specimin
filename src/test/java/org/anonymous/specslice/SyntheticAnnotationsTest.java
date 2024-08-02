package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if synthetic annotations are correctly generated for unsolved annotations. */
public class SyntheticAnnotationsTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "syntheticannotations",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#baz(String)"});
  }
}
