package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks if Specimin can properly remove an unsolved interface.
 */
public class UnsolvedInterfaceTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedinterface",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#doManyThing()"});
  }
}
