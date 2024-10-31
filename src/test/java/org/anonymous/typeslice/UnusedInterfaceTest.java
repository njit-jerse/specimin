package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice can properly remove unused interfaces. */
public class UnusedInterfaceTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "unusedinterface",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#doManyThing()"});
  }
}
