package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks that if TypeSlice will remove comments from the input code */
public class RemovingCommentsTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "removingcomments",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"});
  }
}
