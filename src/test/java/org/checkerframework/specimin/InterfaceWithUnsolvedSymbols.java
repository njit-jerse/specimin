package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test makes sure that Specimin will not crash if an interface contains unsolved symbols.
 */
public class InterfaceWithUnsolvedSymbols {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "interfacewithunsolvedsymbols",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#doSomething()"});
  }
}
