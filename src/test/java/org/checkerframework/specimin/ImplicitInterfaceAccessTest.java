package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks whether Specimin can handle a method from an interface that is implicitly invoked.
 */
public class ImplicitInterfaceAccessTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "implicitinterfaceaccess",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
