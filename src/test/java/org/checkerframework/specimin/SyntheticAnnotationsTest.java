package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks if synthetic annotations are correctly generated for unsolved annotations.
 *
 * <p>Every element here is given a default value except {@code Foo.x()}, whose type is a real
 * annotation type: a nested annotation is usable as a default only if all of <em>its</em> elements
 * have defaults, which Specimin cannot check for a type it did not generate. That asymmetry is
 * harmless in this input, since the single use site of {@code @Foo} supplies {@code x}.
 */
public class SyntheticAnnotationsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "syntheticannotations",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#baz(String)"});
  }
}
