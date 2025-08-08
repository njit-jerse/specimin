package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if Specimin will work if there is an unsolved super constructor in a target
 * method and target constructor.
 */
public class UnsolvedSuperConstructor2Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedsuperconstructor2",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
