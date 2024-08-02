package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if SpecSlice will work for array types */
public class ArrayTypeTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "arrayType",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#sortArray(int[])"});
  }
}
