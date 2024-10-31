package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that given an implicit method call from an unsolved interfaces, if TypeSlice can
 * not figure out which interface that method belongs to, TypeSlice will simply put that method into
 * the synthetic file of the last unsolved interface.
 */
public class ImplicitInterfaceAccessWithManyInterfacesTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "implicitinterfaceaccesswithmanyinterfaces",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
