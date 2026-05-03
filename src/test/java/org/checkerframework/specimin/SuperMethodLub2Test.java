package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks to see if Specimin generates the correct least upper bound return type for an
 * unsolved super method if there are multiple potential return types.
 */
public class SuperMethodLub2Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "supermethodlub2",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
