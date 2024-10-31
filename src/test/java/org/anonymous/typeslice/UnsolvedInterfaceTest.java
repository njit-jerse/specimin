package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice can properly remove an unsolved interface. */
public class UnsolvedInterfaceTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "unsolvedinterface",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#doManyThing()"});
  }
}
