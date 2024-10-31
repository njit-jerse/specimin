package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test ensures that TypeSlice will not create any synthetic file for generic type variables
 * within another type.
 */
public class MapOfTypeVarsTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "mapoftypevars",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
