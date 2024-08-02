package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if the targeted method calls a method imported from JavaParser library,
 * SpecSlice will create a synthetic file for that method and return that file with the targeted
 * file and the targeted method.
 */
public class CallJavaParser {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "callJavaParser",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"});
  }
}
