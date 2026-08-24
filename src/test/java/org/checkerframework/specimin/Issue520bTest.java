package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A method-call variant of <a href="https://github.com/njit-jerse/specimin/issues/520">issue
 * 520</a>: a member accessed on an array whose element type is unsolved. Per JLS 10.7, the members
 * of an array type are {@code length}, {@code clone()}, and the members inherited from {@code
 * Object}, so nothing here needs to be synthesized.
 */
public class Issue520bTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue520b",
        new String[] {"retrofit2/RequestFactory.java"},
        new String[] {"retrofit2.RequestFactory#create(ParameterHandler[])"});
  }
}
