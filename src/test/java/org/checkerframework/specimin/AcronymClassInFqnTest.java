package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin can handle a fully-qualified reference whose simple class name
 * does not have a lowercase second character (e.g. an acronym like {@code UUID}), which the usual
 * class-name-vs-constant-name heuristic rejects. Reduced from
 * https://github.com/njit-jerse/specimin/issues/523.
 */
public class AcronymClassInFqnTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "acronymclassinfqn",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
