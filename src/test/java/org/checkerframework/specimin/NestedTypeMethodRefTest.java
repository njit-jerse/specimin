package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin can handle a method reference whose scope is a qualified,
 * unsolvable nested type ({@code library.Outer.Nested}). The scope's leftmost identifier is {@code
 * Outer}, which is what the import must be matched against (JLS 6.5.5.2); treating the whole name
 * {@code Outer.Nested} as the identifier to look up loses the import.
 */
public class NestedTypeMethodRefTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nestedtypemethodref",
        new String[] {"example/Target.java"},
        new String[] {"example.Target#target()"});
  }
}
