package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks the simplest wildcard import case: there is a single wildcard import, so
 * unsolved classes in the input should be placed there.
 */
public class WildcardImportTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "wildcardimport",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
