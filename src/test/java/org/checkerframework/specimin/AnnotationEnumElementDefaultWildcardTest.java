package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link AnnotationEnumElementDefaultTest}, but the enum is named by a simple name that is not
 * imported by a single-type import, so it could be declared either in the current package (JLS
 * 7.4.1) or in the package imported on demand (JLS 7.5.2). The synthetic enum therefore has more
 * than one candidate name, but only one of them is printed, and the element must still get a
 * default that names a constant of that enum, or the marker use site does not compile (JLS 9.7.1).
 */
public class AnnotationEnumElementDefaultWildcardTest {
  /**
   * Runs the test.
   *
   * @throws IOException if the test files cannot be read
   */
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationenumelementdefaultwildcard",
        new String[] {"org/example/Simple.java"},
        new String[] {"org.example.Simple#target()"});
  }
}
