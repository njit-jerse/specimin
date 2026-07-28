package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin processes the throws clause of a constructor (not just a method),
 * generating the thrown unsolved type as a Throwable subtype.
 */
public class CtorThrowsExceptionTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "ctorthrowsexception",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple()"});
  }
}
