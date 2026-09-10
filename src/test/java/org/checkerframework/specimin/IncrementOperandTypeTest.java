package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Unlike every other unary operator, {@code ++} and {@code --} do not promote: JLS 15.14.2,
 * 15.14.3, 15.15.1 and 15.15.2 undo the promotion with an implicit narrowing cast, so the type of
 * {@code ++x} is the type of {@code x}. What the surrounding context requires of the increment is
 * therefore required of the operand, and a synthetic operand must not simply be given the default
 * numeric type: {@code char c = ++x;} and {@code short s = x++;} do not compile when {@code x} is
 * an {@code int}.
 *
 * <p>The last case pins the boundary. {@code Object} is not a type the operator permits its operand
 * to have, so it is no evidence about the operand and the default is used instead -- which is
 * assignable to {@code Object} by boxing.
 */
public class IncrementOperandTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "incrementoperandtype",
        new String[] {"org/example/Target.java"},
        new String[] {
          "org.example.Target#charPrefix(Metrics)",
          "org.example.Target#shortPostfix(Metrics)",
          "org.example.Target#charReturn(Metrics)",
          "org.example.Target#boxed(Metrics)"
        });
  }
}
