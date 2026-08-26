package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin emits the enclosing type of a synthetic nested type even when the
 * enclosing type is never named in the input. A single-type-import declaration may name a nested
 * type (JLS 7.5.1), so {@code library.Outer} appears nowhere except as the enclosure of {@code
 * Nested}; it still has to be declared for the output to compile.
 */
public class NestedTypeImportedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nestedtypeimported",
        new String[] {"example/Target.java"},
        new String[] {"example.Target#target()"});
  }
}
