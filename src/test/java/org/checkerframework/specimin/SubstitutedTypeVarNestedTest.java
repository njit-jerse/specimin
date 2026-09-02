package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that a type substituted for a type variable keeps its own type arguments, so that they are
 * available to substitute again at the next use. Reading {@code get()} through {@code
 * Container<Container<Absent>>} yields {@code Container<Absent>}, not the erasure {@code
 * Container}; only the former makes the second {@code get()} yield {@code Absent} (JLS 4.5.2).
 */
public class SubstitutedTypeVarNestedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "substitutedtypevarnested",
        new String[] {"com/example/Simple.java", "com/example/Container.java"},
        new String[] {"com.example.Simple#bar(Container<Container<Absent>>)"});
  }
}
