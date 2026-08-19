package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This fixture deliberately pins imprecise output. {@code Dog get()} would be the better answer and
 * it compiles: the return type settles on {@code Dog}, and a {@code Dog} is assignable to {@code
 * Animal a} without conversion, so the earlier assignment needs no repair at all. Specimin emits
 * {@code <T> T get()} anyway.
 *
 * <p>The reason is that the test for "this site is in conflict with the return type" is type
 * equality, not assignability: {@code Dog} is not {@code Animal}, so the {@code Animal} assignment
 * reports a conflict, finds that it cannot be repaired by adding a supertype -- {@code Dog} is a
 * class from the input, so there is nothing of Specimin's to change -- and falls back. Deciding
 * this properly needs {@code UnsolvedSymbolGenerator} to be able to ask whether one type is
 * assignable to another, which needs a type solver it does not currently hold; equality is the
 * approximation that is available, and it errs toward the answer that always compiles.
 *
 * <p><b>If a later change teaches that check about assignability, this expectation should be
 * relaxed to {@code Dog get()}.</b> Doing so is safe and is an improvement, not a regression:
 * nothing here depends on the type variable, and the swapped-statement version in {@link
 * UnrepairableConflictSupertypeTest} -- where the conflict is genuine, because the return type is
 * the supertype and cannot be assigned to the subtype -- is the one that must keep falling back.
 */
public class UnrepairableConflictImpreciseTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unrepairableconflictimprecise",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
