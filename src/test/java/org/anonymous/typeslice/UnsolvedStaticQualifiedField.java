package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if TypeSlice will work if there is an unsolved, static field in a qualified
 * name form used by a target method.
 */
public class UnsolvedStaticQualifiedField {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "unsolvedstaticqualifiedfield",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
