package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if SpecSlice will work if there is an unsolved, static field in a simple
 * name form with a primitive type.
 */
public class UnsolvedStaticSimplePrimitiveField {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "unsolvedstaticsimpleprimitivefield",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
