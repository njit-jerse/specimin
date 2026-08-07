package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Retaining the initializer of a constant used in a constant context must be transitive: the
 * retained initializer is itself a constant expression, so any field it names is also a constant
 * whose own initializer must be retained. Here {@code HEADER_NAME}'s initializer names {@code
 * PREFIX}, so {@code PREFIX} must be preserved with its initializer as well, even though nothing
 * else in the slice mentions it.
 */
public class AnnotationArgConstantTransitiveTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationargconstanttransitive",
        new String[] {"org/example/Target.java"},
        new String[] {"org.example.Target#target()"});
  }
}
