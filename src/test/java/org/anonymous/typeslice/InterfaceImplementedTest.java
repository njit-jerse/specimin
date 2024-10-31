package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice can properly remove unused method signatures from an interface. */
public class InterfaceImplementedTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "interfaceimplemented",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#doSomething()"});
  }
}
