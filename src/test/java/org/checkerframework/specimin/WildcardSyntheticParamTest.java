package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A lambda parameter whose type comes from a wildcard-parameterized functional interface (here,
 * {@code Stream#map}'s {@code Function<? super T, ? extends R>}) must not have that wildcard leak
 * into a synthetic method's formal parameter type: per JLS 15.27.3, the ground target type of an
 * implicitly-typed lambda is the non-wildcard parameterization, so the parameter's type is {@code
 * String}, not {@code ? super String}.
 */
public class WildcardSyntheticParamTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "wildcardsyntheticparam",
        new String[] {"org/example/ExclusiveJoin.java"},
        new String[] {"org.example.ExclusiveJoin#appendIterations(List<String>, int)"});
  }
}
