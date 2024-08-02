package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks that if SpecSlice will remove comments from the input code */
public class RemovingCommentsTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "removingcomments",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"});
  }
}
