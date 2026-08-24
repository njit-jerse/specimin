package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Regression test for <a href="https://github.com/njit-jerse/specimin/issues/522">issue #522</a>.
 */
public class Issue522Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue522",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple(Foo<?>[])"});
  }
}
