package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a method call whose receiver is a pattern variable (JLS 14.30.1) of an
 * unsolved type, here nested inside another such pattern, does not crash Specimin. The input is
 * reduced from the report in https://github.com/njit-jerse/specimin/issues/563. Each pattern type
 * must be castable from the scrutinee's type (JLS 15.20.2), so both are generated as subclasses of
 * it, and each must declare the method called on its pattern variable.
 */
public class Issue563Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue563",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(ConfigurableListableBeanFactory)"});
  }
}
