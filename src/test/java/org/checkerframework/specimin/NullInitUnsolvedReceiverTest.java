package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that Specimin does not try to constrain a variable's type with the NullType resulting from
 * a {@code null} initializer. Regression test for <a
 * href="https://github.com/njit-jerse/specimin/issues/511">#511</a>.
 */
public class NullInitUnsolvedReceiverTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nullinitunsolvedreceiver",
        new String[] {"org/example/Target.java"},
        new String[] {"org.example.Target#target()"});
  }
}
