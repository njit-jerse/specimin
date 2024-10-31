package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice can work for interfaces with generic types. */
public class InterfaceWithGenericTypeTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "interfacewithgenerictype",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#doSomething(String)"});
  }
}
