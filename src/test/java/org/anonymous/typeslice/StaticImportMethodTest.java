package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice can handle an unsolved and statically imported method. */
public class StaticImportMethodTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "staticimportmethod",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"});
  }
}
