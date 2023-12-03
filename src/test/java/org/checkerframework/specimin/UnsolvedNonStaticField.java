package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if Specimin will work if there is an unsolved, non-static field used by
 * target methods
 */
public class UnsolvedNonStaticField {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvednonstaticfield",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#test()"});
  }
}
