package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * JavaParser's {@code Node#equals} is structural, so the two operands of {@code x + x} are equal
 * even though they are distinct nodes. Specimin used to crash on such an expression, because it
 * collected the operands into a {@code Set.of(left, right)}, which rejects duplicates.
 *
 * <p>Structural equality also makes the second operand look like it is already being computed, so
 * the type of the operands has to come from the type of the expression as a whole: {@code int} for
 * the {@code +}, since the enclosing method returns {@code int}, and {@code boolean} for the {@code
 * &}, since JLS 15.22.2 makes a {@code &} whose result is boolean a boolean logical operator.
 */
public class DuplicateBinaryOperandsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "duplicatebinaryoperands",
        new String[] {"org/example/Target.java"},
        new String[] {
          "org.example.Target#twice(Metrics)", "org.example.Target#bothSides(Metrics)"
        });
  }
}
