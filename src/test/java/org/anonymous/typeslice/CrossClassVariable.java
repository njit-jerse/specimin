package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that two simple Java files, one of which uses a global variable coming from the
 * other, are reduced correctly.
 */
public class CrossClassVariable {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "crossclassvariable",
        new String[] {"com/example/Foo.java", "com/example/Baz.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
