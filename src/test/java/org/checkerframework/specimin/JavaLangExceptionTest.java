package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that the {@code Exception} in the throws clause of an anonymous class' override
 * is recognized as {@code java.lang.Exception}, rather than being guessed into the enclosing
 * package. Nothing resolves inside an anonymous class whose supertype is unsolvable, so before
 * issue 525 was fixed, this fell back to the guessing logic, which did not model the implicit
 * {@code import java.lang.*} (JLS 7.5.5) and so named a nonexistent {@code example.Exception}.
 *
 * <p>An override may only throw checked exceptions that the overridden method throws (JLS 8.4.8.3),
 * so {@code java.lang.Exception} is also the right thing for the synthetic supertype to declare.
 */
public class JavaLangExceptionTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "javalangexception",
        new String[] {"example/Target.java"},
        new String[] {"example.Target#target()"});
  }
}
