package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks if constructor stubs are properly generated for types found in permits clauses.
 */
public class PermitsConstructorsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "permitsconstructors",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#foo()"});
  }
}
