package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** The test for <a href="https://github.com/kelloggm/specSlice/issues/185">...</a>. */
public class OverrideDoubleTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "overridedouble",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#setFrame(Frame)"});
  }
}
