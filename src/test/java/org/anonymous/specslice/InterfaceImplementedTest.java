package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if SpecSlice can properly remove unused method signatures from an interface. */
public class InterfaceImplementedTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "interfaceimplemented",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#doSomething()"});
  }
}
