package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that types in permits clauses are preserved, in addition to relevant keywords
 * like final, sealed, and non-sealed in child classes.
 */
public class PermitsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "permits", new String[] {"com/example/Foo.java"}, new String[] {"com.example.Foo#foo()"});
  }
}
