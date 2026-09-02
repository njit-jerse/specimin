package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Two uses of the same element of a synthetic annotation type, one supplying a {@code String}
 * constant from the input and the other a name Specimin would otherwise guess is an enum constant.
 *
 * <p>An annotation type declares each element only once (JLS 9.6), so the two must agree. The
 * {@code String} constant decides: its type is one Specimin reads out of the input rather than
 * invents, and JLS 9.7.1 then requires the element's type to be {@code String}. That leaves no room
 * for the enum guess, so the external name is a constant variable (JLS 4.12.4) of the same type.
 *
 * <p>{@link AnnotationArgEnumAndConstantTest} is the same input with the two arguments swapped;
 * Specimin reaches them in the opposite order, so the two tests exercise different paths.
 */
public class AnnotationArgConstantAndEnumTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationargconstantandenum",
        new String[] {"org/example/ApplicationsResource.java"},
        new String[] {"org.example.ApplicationsResource#getContainers(String, String)"});
  }
}
