package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks the case where there is a wildcard import, but there is a matching
 * class in the same package as the target class. In that case, the wildcard import should
 * be ignored.
 */
public class WildcardImport4Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "wildcardimport4",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
