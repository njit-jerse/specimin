package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that different parameter and argument types (e.g., ArrayList as the argument and
 * List as the parameter) don't cause Specimin to mistakenly identify a method as not relevant.
 */
public class DifferentParamTypesTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "differentparamtypes",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
