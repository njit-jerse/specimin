package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks whether Specimin can handle the mismatching types when the return type of a
 * synthetic method is used as a parameter of an existing method in a complicated way.
 */
public class MismatchTypeParameterHardCaseTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "mismatchtypeparameterhardcase",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(List<String>)"});
  }
}
