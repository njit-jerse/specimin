package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link ConcatOfUnsolvedTest}, but for the compound assignment {@code s += x} on a {@code
 * String} target rather than the binary {@code "" + x}. JLS 15.26.2 defines {@code s += x} as
 * {@code s = (String) (s + x)}, so the right-hand side is a string concatenation operand and is
 * therefore unconstrained: it must not be forced to be a subtype of the final class {@code String}.
 */
public class PlusAssignOfUnsolvedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "plusassignofunsolved",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
