package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice will work for array types */
public class ArrayTypeTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "arrayType",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#sortArray(int[])"});
  }
}
