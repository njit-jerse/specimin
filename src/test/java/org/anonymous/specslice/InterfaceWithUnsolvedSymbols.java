package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test makes sure that SpecSlice will not crash if an interface contains unsolved symbols. */
public class InterfaceWithUnsolvedSymbols {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "interfacewithunsolvedsymbols",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#doSomething(String)"});
  }
}
