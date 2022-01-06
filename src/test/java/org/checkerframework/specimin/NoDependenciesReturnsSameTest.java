package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that a simple Java file with no dependencies whatsoever and a single target
 * method is returned unaltered by specimin.
 */
public class NoDependenciesReturnsSameTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTest("nodependenciesreturnssame", "com/example/Simple.java");
  }
}
