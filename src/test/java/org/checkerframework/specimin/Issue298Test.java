package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test is a simplified version of the problem described by <a
 * href="https://github.com/njit-jerse/specimin/issues/298">...</a>. The test is somewhat limited -
 * it triggers part of the problem, but not all of it: in particular, the small test case doesn't
 * trigger the "resolved yet stuck method call" code in {@link TargetMethodFinderVisitor}, which is
 * part of the real bug.
 */
public class Issue298Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue298",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#isSideEffectFree(ExecutableElement)"});
  }
}
