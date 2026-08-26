package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that making a synthetic type extend {@code Throwable} corrects the parameter
 * types of synthetic <em>constructors</em>, not only of synthetic methods. Catching {@code
 * UnsolvedException} forces it to extend {@code Throwable} (JLS 11.2), at which point {@code
 * getMessage()} is Throwable's and returns {@code String}, so the placeholder Specimin invented for
 * its result is withdrawn. The constructor that took that placeholder as a parameter has to be
 * corrected too, or it names a type that is never emitted.
 */
public class ConstructorParamThrowableCorrectedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "constructorparamthrowablecorrected",
        new String[] {"example/Target.java"},
        new String[] {"example.Target#target()"});
  }
}
