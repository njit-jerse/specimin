package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin produces a compilable constructor when it must synthesize a nested
 * class (here, {@code library.Outer.Nested}) whose constructor is invoked by the target method. A
 * constructor's name is a simple name (JLS 8.8.1), so the synthetic constructor must be named
 * {@code Nested}, not {@code Outer.Nested}.
 */
public class NestedConstructorTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nestedconstructor",
        new String[] {"example/Target.java"},
        new String[] {"example.Target#target()"});
  }
}
