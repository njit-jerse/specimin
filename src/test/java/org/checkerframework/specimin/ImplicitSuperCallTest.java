package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks whether Specimin can handle a method from a superclass that is implicitly invoked.
 */
public class ImplicitSuperCallTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "implicitinterfaceaccess",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
