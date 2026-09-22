package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that the {@code null} literal on the other side of an unsolved expression never
 * becomes that expression's type. Under {@code ==} and {@code !=}, in either operand order and for
 * fields as well as methods, the comparison only requires a reference type (JLS 15.21.3), so {@code
 * Object} is correct, and a later member access on the result still gets a placeholder. When the
 * other branch of a conditional is {@code null}, the conditional is a poly expression whose
 * operands must each be compatible with its target type (JLS 15.25.3), so the unsolved branch takes
 * the type of the conditional's own context. That type may be primitive: {@code flag ? getCount() :
 * null} has type {@code Integer} (JLS 15.25), so {@code int getCount()} is fine when the
 * conditional is unboxed to {@code int}.
 */
public class NullComparisonVariantsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nullcomparisonvariants",
        new String[] {"com/example/Simple.java"},
        new String[] {
          "com.example.Simple#mirror(Ctx)",
          "com.example.Simple#field(Ctx)",
          "com.example.Simple#conditional(Ctx, boolean)",
          "com.example.Simple#primitiveConditional(Ctx, boolean)",
          "com.example.Simple#memberAfterComparison(Ctx)"
        });
  }
}
