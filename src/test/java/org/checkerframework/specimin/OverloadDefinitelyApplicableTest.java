package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that, when one overload is definitely applicable to a call and another is only
 * possibly applicable, Specimin preserves the definitely applicable one regardless of declaration
 * order.
 *
 * <p>{@code Sub} implements {@code Target}, so {@code clear(Target)} is applicable (JLS 15.12.2.2).
 * {@code clear(View)} is applicable only if the unsolvable {@code Fragment} is a subtype of {@code
 * View}; even then, neither overload would be more specific than the other (JLS 15.12.2.5) and the
 * call would be ambiguous. Since the input compiles, javac must select {@code clear(Target)}, and
 * {@code clear(View)} is omitted along with its import.
 */
public class OverloadDefinitelyApplicableTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "overloaddefinitelyapplicable",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo(Sub)"});
  }
}
