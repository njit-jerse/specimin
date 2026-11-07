package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin generates the type of a pattern variable (JLS 14.30.1) that is
 * used as the receiver of an unsolved method call. This is the minimized trigger for
 * https://github.com/njit-jerse/specimin/issues/563: the scrutinee's type is irrelevant, and the
 * crash requires only that the pattern's type be unsolved and that the pattern variable be a
 * method-call receiver.
 */
public class PatternVariableReceiverTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "patternvariablereceiver",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Object)"});
  }
}
