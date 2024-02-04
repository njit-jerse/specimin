package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test makes sure that Specimin will not crash if an interface contains unsolved symbols. */
public class InterfaceWithUnsolvedSymbols {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "interfacewithunsolvedsymbols",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#doSomething(String)"});
  }
}
