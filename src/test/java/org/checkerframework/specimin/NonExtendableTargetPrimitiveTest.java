package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Two assignments of the same unsolved call compete: one to an {@code int} and one to a class
 * Specimin generates a subtype for. A primitive has no subtypes at all, so no synthetic return type
 * can serve both, and the only return type that satisfies both is an unconstrained type variable:
 * {@code T} is inferred as {@code Integer} and unboxed at the first assignment (JLS 5.2), and as
 * {@code Payload} at the second.
 *
 * <p>This is the assignment counterpart of {@link LambdaResultUnsolvedArrayTargetTest} and its
 * siblings, which cover the same competition when the target types come from lambdas. Those reach
 * the fallback by name; an assignment reaches it through the left-hand side's resolved type, which
 * is a separate decision in {@code UnsolvedSymbolGenerator#isNonExtendableType}. See
 * https://github.com/njit-jerse/specimin/issues/509.
 *
 * <p>{@link NonExtendableTargetNoConflictTest} covers the other half of that decision: a
 * non-extendable target that no other use site competes with keeps its precise return type.
 */
public class NonExtendableTargetPrimitiveTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nonextendabletargetprimitive",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
