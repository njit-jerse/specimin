package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test makes sure that TypeSlice will not crash if methods from interfaces have unsolved
 * return types.
 */
public class InterfaceMethodWithUnsolvedTypeTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "interfacemethodwithunsolvedtype",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#doSomething(String)"});
  }
}
