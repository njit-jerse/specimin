package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks for a crash related to the one in
 * https://github.com/njit-jerse/specimin/issues/86. The difference between this and {@link
 * OtherPackageArrayTest} is that in this version, the called method is a constructor (which was
 * required for the actual crash reported in the issue). The actual crash is more complex, because
 * it involves the Type class being in a different package; see {@link OtherPackageArray3Test}
 */
public class OtherPackageArray2Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "otherpackagearray2",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(org.example.Method)"});
  }
}
