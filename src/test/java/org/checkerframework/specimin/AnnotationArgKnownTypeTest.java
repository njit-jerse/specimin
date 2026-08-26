package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * When the annotation type is in the input, its element's declared type says exactly what an
 * unsolved constant used as that element's value must be (JLS 9.7.1), so Specimin need not guess.
 * Here {@code Header} declares {@code String value()}, so {@code HEADER_NAME} is a {@code String}
 * constant variable rather than a type Specimin invents.
 *
 * <p>Being a constant variable also dictates the declaration: JLS 4.12.4 requires it to be final,
 * with a constant initializer, and reaching it through a static import requires it to be static.
 * The usual {@code null} default Specimin gives a reference-typed field is not a constant
 * expression, so a string literal is used instead.
 */
public class AnnotationArgKnownTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationargknowntype",
        new String[] {"org/example/Target.java"},
        new String[] {"org.example.Target#target(String)"});
  }
}
