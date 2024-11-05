package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if Specimin correctly preserves things that are used as arguments to enum
 * constants.
 */
public class DefaultPackageTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "defaultpackage", new String[] {"A.java"}, new String[] {"A#schedule(Runnable, int)"});
  }
}
