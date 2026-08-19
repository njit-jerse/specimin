package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * {@link NonExtendableCastOperandPrimitiveTest}'s program with its two statements swapped, so that
 * the assignment to the {@code int} is examined first. Both orders must compile; this one is what
 * guards the claim that the outcome does not depend on which statement comes first.
 *
 * <p>The two orders reach the same return type by different routes, so the rest of the output
 * differs. Here the assignment is examined while the placeholder return type is still in place, so
 * the assignment itself triggers the fallback and the cast then finds an unconstrained operand type
 * and leaves {@code Baz} extending the placeholder. That keeps an otherwise empty class in the
 * output, which is the usual trade: minimality is worth giving up for compilability.
 */
public class NonExtendableCastOperandReversedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nonextendablecastoperandreversed",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
