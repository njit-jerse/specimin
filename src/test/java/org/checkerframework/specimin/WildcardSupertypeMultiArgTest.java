package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link WildcardSupertypeTest}, but the supertype has more than one type argument and every
 * one of them is a wildcard. Each must be replaced independently.
 */
public class WildcardSupertypeMultiArgTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "wildcardsupertypemultiarg",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Bar)"});
  }
}
