package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks the case where there is a wildcard import, but there is a matching class in the
 * same package as the target class. In that case, the wildcard import should be ignored.
 */
public class WildcardImport4Test {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "wildcardimport4",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
