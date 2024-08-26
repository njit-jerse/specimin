package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin can properly remove unused imports. */
public class UnusedImportsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unusedimports",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#shouldNotBeRemoved()"});
  }
}
