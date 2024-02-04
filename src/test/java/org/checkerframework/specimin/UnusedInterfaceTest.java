package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks if Specimin can properly remove unused interfaces.
 */
public class UnusedInterfaceTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unusedinterface",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#doManyThing()"});
  }
}
