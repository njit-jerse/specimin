package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks the default value that a synthetic annotation type's element gets for each of the element
 * types that Specimin can name a value for: a primitive, {@code String}, {@code Class}, and an
 * array. The marker use site is what makes all four defaults necessary (JLS 9.7.2).
 *
 * <p>The two remaining legal element types (JLS 9.6.1) -- an enum type and an annotation type --
 * are deliberately absent, because Specimin does not default them; see {@link
 * org.checkerframework.specimin.unsolved.SpeciminGenerationUtils#getAnnotationElementDefaultValue}.
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
