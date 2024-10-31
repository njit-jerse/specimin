package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks whether TypeSlice can handle a method from an interface that is implicitly
 * invoked.
 */
public class ImplicitInterfaceAccessTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "implicitinterfaceaccess",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
