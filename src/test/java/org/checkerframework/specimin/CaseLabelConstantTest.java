package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A {@code case} label is a constant context, just like an annotation argument, so a field used as
 * one must retain its initializer. Replacing the initializer of a {@code String} constant with
 * {@code null} (as Specimin usually does when preserving a final field) makes the {@code case}
 * label a non-constant expression, which does not compile.
 */
public class CaseLabelConstantTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "caselabelconstant",
        new String[] {"org/example/Target.java"},
        new String[] {"org.example.Target#target(String)"});
  }
}
