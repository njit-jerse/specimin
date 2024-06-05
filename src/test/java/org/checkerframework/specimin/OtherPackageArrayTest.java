package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks for the crash in https://github.com/njit-jerse/specimin/issues/86. */
public class OtherPackageArrayTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "otherpackagearray",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(org.example.Method)"});
  }
}
