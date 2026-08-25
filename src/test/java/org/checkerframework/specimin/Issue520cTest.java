package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A method-reference variant of <a href="https://github.com/njit-jerse/specimin/issues/520">issue
 * 520</a>: a method reference whose scope is an array whose element type is unsolved. JLS 10.7
 * gives an array type only {@code length}, {@code clone()}, and the members inherited from {@code
 * Object}, and JLS 15.13 makes {@code T[]::new} an array creation reference rather than a reference
 * to a declared constructor, so neither form has anything for Specimin to synthesize.
 */
public class Issue520cTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue520c",
        new String[] {"retrofit2/RequestFactory.java"},
        new String[] {"retrofit2.RequestFactory#create(ParameterHandler[])"});
  }
}
