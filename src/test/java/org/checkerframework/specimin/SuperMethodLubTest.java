package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks to see if Specimin generates the correct least upper bound return type for an
 * unsolved super method if child overrides are known and have different return types.
 */
public class SuperMethodLubTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "supermethodlub",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
