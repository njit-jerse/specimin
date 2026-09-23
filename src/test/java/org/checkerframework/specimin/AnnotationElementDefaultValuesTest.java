package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks the default value that a synthetic annotation type's element gets for each of the element
 * types that Specimin can name a value for: a primitive, {@code String}, {@code Class}, an array,
 * and a synthetic enum. The marker use site is what makes all five defaults necessary (JLS 9.7.2).
 * The enum-typed element defaults to a constant of its enum that is already in the output.
 *
 * <p>TODO: Other legal forms: an annotation type, a solved enum type, or a bounded Class type like
 * {@code Class<? extends Number>} are not tested, because Specimin does not currently support them.
 * Add them to this test when support for them is added.
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
