package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A constant variable does not have to be {@code static}: JLS 4.12.4 requires only a <em>final</em>
 * variable of primitive or {@code String} type initialized with a constant expression. So the
 * transitive rule exercised by {@link AnnotationArgConstantTransitiveTest} must apply to instance
 * fields too. Here {@code HEADER_NAME} is a non-static final field used as an annotation argument,
 * and its initializer names the non-static final field {@code PREFIX}, which must therefore keep
 * its own initializer.
 */
public class InstanceConstantTransitiveTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "instanceconstanttransitive",
        new String[] {"org/example/Target.java"},
        new String[] {"org.example.Target#target()"});
  }
}
