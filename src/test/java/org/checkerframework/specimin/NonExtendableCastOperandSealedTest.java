package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The sealed counterpart of {@link NonExtendableCastOperandPrimitiveTest}. The cast's operand has
 * type {@code SealedBase}, which permits only {@code Known} (JLS 8.1.1.2), so the synthetic {@code
 * Baz} cannot be made a subtype of it and the return type falls back instead. Without that, the
 * output declares {@code Baz extends SealedBase}, which does not compile.
 */
public class NonExtendableCastOperandSealedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nonextendablecastoperandsealed",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
