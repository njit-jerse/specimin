package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that casting the result of an unsolved method does not degrade the return type inferred
 * for that method at its other use sites. Here the cast target is {@code Object}, which constrains
 * nothing at all, so the assignment to a variable of type {@code Bar} should still force {@code
 * get()} to return {@code Bar}.
 */
public class CastAndContextHarmlessTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "castandcontextharmless",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
