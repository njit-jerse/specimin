package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

public class CustomExceptionTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "customexception",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"});
  }
}
