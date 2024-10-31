package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test makes sure that TypeSlice will not crash if an interface contains unsolved symbols. */
public class InterfaceWithUnsolvedSymbols {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "interfacewithunsolvedsymbols",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#doSomething(String)"});
  }
}
