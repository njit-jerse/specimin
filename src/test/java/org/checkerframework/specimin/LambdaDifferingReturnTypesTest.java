package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A lambda with several value returns of differing types, here {@code Dog} and {@code Cat}.
 *
 * <p>No least upper bound is computed, and none is needed: a synthetic functional interface always
 * takes an unbounded wildcard as its return type argument, so {@code map} gets {@code Function<?
 * extends SyntheticTypeForItem, ?>} whatever the lambda returns. That is safe rather than merely
 * imprecise — the ground target type of {@code Function<X, ?>} is {@code Function<X, Object>}, so
 * every return expression is assignable regardless of type.
 *
 * <p>This pins that behavior: deriving a return type from the return expressions instead would
 * change this output, and would need to justify itself against the wildcard, which cannot fail to
 * compile.
 */
public class LambdaDifferingReturnTypesTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdadifferingreturntypes",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Wrapper, Dog, Cat)"});
  }
}
