package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A multi-catch parameter used as an expression. JLS 14.20 gives it the lub of the alternatives;
 * Specimin does not compute lubs, so it reports java.lang.Throwable, the bound JLS 11.1.1
 * guarantees for every alternative. Reporting one alternative instead would be unsound.
 */
public class MultiCatchParamTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "multicatchparamtype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target()"});
  }
}
