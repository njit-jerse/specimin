package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks that SpecSlice can handle method overriding correctly. */
public class OverridingMethodTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "overridingmethod",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(int, UnsolvedType)"});
  }
}
