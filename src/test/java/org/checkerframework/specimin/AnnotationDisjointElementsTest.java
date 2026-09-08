package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Two use sites of the same synthetic annotation type supply disjoint sets of elements, so each
 * element is omitted by one of them and both therefore need defaults (JLS 9.7.1). Unlike {@link
 * AnnotationElementWithoutDefaultTest}, neither use site is a marker or single-element annotation,
 * which shows that it is the omission and not the shorthand that requires the default.
 */
public class AnnotationDisjointElementsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationdisjointelements",
        new String[] {"org/example/Simple.java"},
        new String[] {"org.example.Simple#target()"});
  }
}
