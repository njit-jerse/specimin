package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Regression test case for <a href="https://github.com/njit-jerse/specimin/issues/520">issue
 * 520</a>.
 */
public class Issue520Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue520",
        new String[] {"retrofit2/RequestFactory.java"},
        new String[] {"retrofit2.RequestFactory#create(Object[])"});
  }
}
