package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks the case where the type substituted for a type variable is itself a type variable that
 * needs substituting. {@code Mid<E> extends Container<E>} substitutes {@code Container}'s {@code T}
 * by {@code E} (JLS 8.4.8), and the receiver {@code Mid<Absent>} substitutes that {@code E} by
 * {@code Absent} (JLS 4.5.2); only composing the two makes {@code m.get()} an {@code Absent}.
 *
 * <p>This is the link that {@link SubstitutedTypeVarFromSupertypeTest} does not exercise, because
 * there the supertype's type argument is already the unsolved type rather than a variable standing
 * for it.
 */
public class TypeVarSubstitutedByTypeVarTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarsubstitutedbytypevar",
        new String[] {
          "com/example/Simple.java", "com/example/Mid.java", "com/example/Container.java"
        },
        new String[] {"com.example.Simple#bar(Mid<Absent>)"});
  }
}
