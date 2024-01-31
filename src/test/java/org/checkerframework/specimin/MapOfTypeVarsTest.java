package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test ensures that Specimin will not create any synthetic file for generic type variables
 * within another type.
 */
public class MapOfTypeVarsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "mapoftypevars",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
