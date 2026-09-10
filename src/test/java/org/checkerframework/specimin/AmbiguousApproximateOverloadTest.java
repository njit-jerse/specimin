package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link SameSimpleNameOverloadTest}, but neither of {@code Impl}'s two overloads can be
 * matched exactly: {@code Rule#apply}'s own parameter type is off the source path, so no signature
 * in the comparison can be computed and both overloads match {@code apply(Foo)} approximately.
 *
 * <p>Specimin cannot tell which of the two implements {@code Rule#apply}, so it preserves both.
 * Preserving the wrong one alone would leave {@code Impl} without an implementation of an abstract
 * method it inherits, which does not compile (JLS 8.1.1.1); preserving both only costs minimality,
 * which Specimin trades away for compilability. This test pins that policy -- it also passes
 * against a version that picks an arbitrary one of the two, because several passes over {@code
 * Impl} happen to pick different ones and the slice is their union, so what it really guards
 * against is a future change that resolves the ambiguity by preserving neither.
 */
public class AmbiguousApproximateOverloadTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "ambiguousapproximateoverload",
        new String[] {"com/example/Target.java"},
        new String[] {"com.example.Target#target(Foo)"});
  }
}
