package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A field whose value is used as an annotation argument must retain its initializer: annotation
 * arguments must be non-null compile-time constants, so replacing the initializer with {@code null}
 * (as Specimin usually does when preserving a field) produces uncompilable output.
 */
public class AnnotationArgConstantTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationargconstant",
        new String[] {"org/example/Target.java"},
        new String[] {"org.example.Target#target()"});
  }
}
