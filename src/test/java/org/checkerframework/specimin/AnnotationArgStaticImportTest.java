package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A statically-imported constant used as an annotation argument. JLS 9.7.1 permits only two kinds
 * of name in that position: an enum constant, or a constant variable (whose type is a primitive or
 * {@code String}, JLS 4.12.4). Neither the annotation type nor the constant's declaring type is in
 * the input, so Specimin must invent both, and it guesses that the constant is an enum constant.
 *
 * <p>Specimin used to apply this rule only to a qualified name such as {@code Constants.X}, so a
 * bare name reaching its declaration through a static import got an invented type of its own, which
 * is neither an allowable annotation argument nor an allowable annotation element type. See <a
 * href="https://github.com/njit-jerse/specimin/issues/524">issue 524</a>.
 */
public class AnnotationArgStaticImportTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationargstaticimport",
        new String[] {"org/example/Target.java"},
        new String[] {"org.example.Target#target(String)"});
  }
}
