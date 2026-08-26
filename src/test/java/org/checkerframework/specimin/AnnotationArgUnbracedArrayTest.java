package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The unbraced counterpart of {@link AnnotationArgKnownArrayTypeTest}. JLS 9.7.1 lets an
 * array-typed element take a single value without braces, as shorthand for a one-element array, so
 * {@code HEADER_NAME} in {@code @Header(HEADER_NAME)} is constrained to {@code String} by a {@code
 * String[] value()} element just as it would be inside braces.
 *
 * <p>Reading the declared {@code String[]} as this value's own type instead leaves the name looking
 * unconstrained, which sends it to the guess that a name in an annotation argument is an enum
 * constant -- and an enum constant cannot be converted to the declared {@code String}.
 */
public class AnnotationArgUnbracedArrayTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationargunbracedarray",
        new String[] {"org/example/Target.java"},
        new String[] {"org.example.Target#target(String)"});
  }
}
