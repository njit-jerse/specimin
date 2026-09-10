package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Every unary operator constrains its operand's type, so a synthetic member used as one must be
 * given a type the operator accepts: boolean for {@code !} (JLS 15.15.6), an integral type for
 * {@code ~} (JLS 15.15.5), and a numeric type for {@code -} and {@code ++} (JLS 15.15.4, 15.14.2).
 * Specimin used to ignore unary expressions entirely, so the operand kept whatever synthetic type
 * it would have had in an unconstraining context, and the output did not compile.
 */
public class UnaryOperandTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unaryoperandtype",
        new String[] {"org/example/Target.java"},
        new String[] {
          "org.example.Target#createDefaultApplicationContext()",
          "org.example.Target#mask(Flags)",
          "org.example.Target#tick(Counter)",
          "org.example.Target#negate(Counter)"
        });
  }
}
