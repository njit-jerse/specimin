package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks for the actual crash in https://github.com/njit-jerse/specimin/issues/86. The
 * difference between this and {@link OtherPackageArray2Test} is that in this version, the Type
 * class is in a different package than the Method class, which was required to evoke the real
 * crash.
 */
public class OtherPackageArray3Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "otherpackagearray3",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(org.example.Method)"});
  }
}
