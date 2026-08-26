package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The qualified-name counterpart of {@link AnnotationArgKnownTypeTest}: {@code Constants} must stay
 * an ordinary class holding a {@code String} constant, because {@code Header} declares {@code
 * String value()}.
 *
 * <p>Specimin guesses that the declaring type of a constant used as an annotation argument is an
 * enum, since an enum constant is the other thing JLS 9.7.1 allows there. That guess used to be
 * applied even when the annotation type was in the input and ruled it out, producing an {@code enum
 * Constants} that could not be converted to the declared {@code String}.
 */
public class AnnotationArgKnownTypeQualifiedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationargknowntypequalified",
        new String[] {"org/example/Target.java"},
        new String[] {"org.example.Target#target(String)"});
  }
}
