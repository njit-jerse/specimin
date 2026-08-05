package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that a synthetic class that is assigned to a variable whose synthetic type is used with a
 * wildcard type argument is still made a subtype of that type. A wildcard cannot appear in an
 * extends clause, so the wildcard must be replaced by a concrete type argument rather than the
 * supertype relationship being discarded.
 */
public class WildcardSupertypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "wildcardsupertype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Bar)"});
  }
}
