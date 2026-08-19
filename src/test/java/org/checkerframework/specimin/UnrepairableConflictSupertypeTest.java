package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Neither assignment target here is non-extendable, so nothing in this program is about finality:
 * the return type settles on {@code Animal}, and {@code Dog d = item.get();} then asks for a
 * narrowing reference conversion, which an assignment context does not perform (JLS 5.2). The
 * conflict is real and there is no supertype to add -- {@code Animal} is a class from the input,
 * not something Specimin generated -- so an unconstrained return type is what satisfies both
 * assignments, with {@code T} inferred as {@code Dog} at the first and {@code Animal} at the
 * second.
 *
 * <p>{@link UnrepairableConflictImpreciseTest} is this program with the two statements swapped,
 * where the return type lands on the subtype instead and no repair is actually needed.
 */
public class UnrepairableConflictSupertypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unrepairableconflictsupertype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
