package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that given an implicit method call from an unsolved interfaces, if Specimin can
 * not figure out which interface that method belongs to, Specimin will simply put that method into
 * the synthetic file of the last unsolved interface.
 */
public class ImplicitInterfaceAccessWithManyInterfacesTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "implicitinterfaceaccesswithmanyinterfaces",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
