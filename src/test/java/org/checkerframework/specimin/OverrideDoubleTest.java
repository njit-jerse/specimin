package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** The test for <a href="https://github.com/kelloggm/specimin/issues/185">...</a>. */
public class OverrideDoubleTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "overridedouble",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#setFrame(Frame)"});
  }
}
