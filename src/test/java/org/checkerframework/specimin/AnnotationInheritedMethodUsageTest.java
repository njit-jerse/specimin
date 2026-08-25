package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link AnnotationMethodUsageInArgumentTest}, but the calls are to methods that an annotation
 * type inherits from {@code java.lang.annotation.Annotation} rather than to one of its own members.
 */
public class AnnotationInheritedMethodUsageTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationinheritedmethodusage",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo(Anno)"});
  }
}
