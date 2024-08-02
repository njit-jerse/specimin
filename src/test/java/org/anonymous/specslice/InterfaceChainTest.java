package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if a class implements an interface which implements another interface, the
 * methods that need to be implemented still are (to preserve compilability). Test code partially
 * derived from minimized test code in NullAway issue #102.
 */
public class InterfaceChainTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "interfacechain",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#iterator()"});
  }
}
