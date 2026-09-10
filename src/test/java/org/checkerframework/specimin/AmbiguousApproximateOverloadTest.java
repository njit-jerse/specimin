package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link SameSimpleNameOverloadTest}, but neither of {@code Impl}'s two overloads can be
 * matched exactly: {@code Rule#apply}'s own parameter type is off the source path, so no signature
 * in the comparison can be computed and both overloads match {@code apply(Foo)} approximately.
 *
 * <p>Specimin cannot tell which of the two implements {@code Rule#apply}, so it should preserve both.
 * Preserving the wrong one alone would leave {@code Impl} without an implementation of an abstract
 * method it inherits, which does not compile (JLS 8.1.1.1); preserving both only costs minimality.
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
