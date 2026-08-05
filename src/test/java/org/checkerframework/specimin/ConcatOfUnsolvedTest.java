package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that appearing as an operand of a string concatenation constrains an expression's type in
 * no way. String concatenation accepts an operand of any type at all, so {@code "" + f.get()} tells
 * Specimin nothing about what {@code get()} returns, and the cast on the previous line must remain
 * free to establish it.
 */
public class ConcatOfUnsolvedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "concatofunsolved",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
