package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A synthetic annotation type that is used both with and without an argument needs a default value
 * for the corresponding element, or the argument-less use site does not compile (JLS 9.7.1).
 */
public class AnnotationElementWithoutDefaultTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annotationelementwithoutdefault",
        new String[] {"org/example/ClassInfo.java"},
        new String[] {"org.example.ClassInfo#getPackage()"});
  }
}
