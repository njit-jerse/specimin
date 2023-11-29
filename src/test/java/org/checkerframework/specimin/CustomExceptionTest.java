package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

public class CustomExceptionTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "customexception",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"});
  }
}
