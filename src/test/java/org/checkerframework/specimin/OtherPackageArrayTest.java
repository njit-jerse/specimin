package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks for a simpler variant of the crash in
 * https://github.com/njit-jerse/specimin/issues/86. This test doesn't reproduce the crash directly,
 * but it does trigger a related bug that also needed to be fixed to avoid the crash (related to
 * arrays in type names).
 */
public class OtherPackageArrayTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "otherpackagearray",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(org.example.Method)"});
  }
}
