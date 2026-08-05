package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This variant of {@link SuperWildcardFinalBoundTest} has a type variable of a synthetic type on
 * the RHS of the second assignment, inducing a subtyping between two synthetic classes.
 */
public class SuperWildcardFinalBound2Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "superwildcardfinalbound2",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Bar)"});
  }
}
