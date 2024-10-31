package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if TypeSlice will work if there is an unsolved, non-static field used by
 * target methods
 */
public class UnsolvedNonStaticField {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "unsolvednonstaticfield",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#test()"});
  }
}
