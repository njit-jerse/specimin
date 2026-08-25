package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a fully-qualified type whose class name is an acronym is generated in the
 * package the source names, rather than in a package nested under the current one.
 */
public class AcronymClassInTypePositionTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "acronymclassintypeposition",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
