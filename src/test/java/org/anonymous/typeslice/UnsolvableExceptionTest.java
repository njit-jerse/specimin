package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

public class UnsolvableExceptionTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "unsolvedexception",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"});
  }
}
