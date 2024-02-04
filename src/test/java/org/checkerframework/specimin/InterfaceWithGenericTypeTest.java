package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks if Specimin can work for interfaces with generic types.
 */
public class InterfaceWithGenericTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "interfacewithgenerictype",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#doSomething()"});
  }
}
