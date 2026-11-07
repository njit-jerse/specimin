package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A synthetic annotation type whose element is enum-typed, used once with a value and once without.
 * The statically-imported {@code JOIN} is guessed to be an enum constant (see {@link
 * AnnotationArgStaticImportTest}), which makes {@code value()} an element of the synthetic enum
 * type {@code TaskType}. The marker annotation {@code @Component} on {@code Executor} omits that
 * element, which JLS 9.7.1 permits only if the element has a default, so the output must declare
 * one. Any constant of {@code TaskType} is commensurate with the element type (JLS 9.6.2, 9.7.1),
 * and an element value cannot appear in a constant expression, so no typing judgment about the
 * target depends on which constant is chosen; the one already in the output is used. See <a
 * href="https://github.com/njit-jerse/specimin/issues/560">issue 560</a>.
 */
public class AnnotationEnumElementDefaultTest {
  /**
   * Runs the test.
   *
   * @throws IOException if the test files cannot be read
   */
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationenumelementdefault",
        new String[] {"org/example/Join.java"},
        new String[] {"org.example.Join#execute(Executor)"});
  }
}
