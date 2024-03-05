package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks the simplest wildcard import case: there is a single wildcard import,
 * so unsolved classes in the input should be placed there.
 */
public class WildcardImportTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "wildcardimport",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
