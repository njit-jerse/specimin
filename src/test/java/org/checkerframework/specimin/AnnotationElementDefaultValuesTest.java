package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks the default value that a synthetic annotation type's element gets for each of the element
 * types that Specimin can name a value for: a primitive, {@code String}, {@code Class}, and an
 * array. The marker use site is what makes all four defaults necessary (JLS 9.7.2).
 *
 * <p>TODO: Other legal forms: an enum type, an annotation type, or a bounded Class type like
 * {@code Class<? extends Number>} are not tested, because Specimin does not currently support
 * them. Add them to this test when support for them is added.
 */
public class AnnotationElementDefaultValuesTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationelementdefaultvalues",
        new String[] {"org/example/Simple.java"},
        new String[] {"org.example.Simple#target()"});
  }
}
