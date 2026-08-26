package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * An array-valued annotation element constrains each value in its array initializer to the array's
 * component type (JLS 9.7.1), not to the array type itself. {@code Header} declares {@code String[]
 * value()}, so the unsolved {@code HEADER_NAME} inside {@code @Header({HEADER_NAME})} is a {@code
 * String} constant.
 */
public class AnnotationArgKnownArrayTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationargknownarraytype",
        new String[] {"org/example/Target.java"},
        new String[] {"org.example.Target#target(String)"});
  }
}
