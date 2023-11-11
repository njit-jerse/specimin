package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if Specimin will work for array types
 */
public class ArrayTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "arrayType",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#sortArray(int[])"});
  }
}
