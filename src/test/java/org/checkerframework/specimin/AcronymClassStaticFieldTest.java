package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin generates the right class for a static field read through a
 * fully-qualified name whose class name is an acronym. The whole name is in expression position, so
 * by JLS 6.5.2 its last identifier is a field and everything to its left is a type name.
 */
public class AcronymClassStaticFieldTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "acronymclassstaticfield",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
