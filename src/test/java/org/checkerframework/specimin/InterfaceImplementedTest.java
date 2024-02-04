package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks if Specimin can properly remove unused method signatures from an interface.
 */
public class InterfaceImplementedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedinterface",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#doSomething()"});
  }
}
