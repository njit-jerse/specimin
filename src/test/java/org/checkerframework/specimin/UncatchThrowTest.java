package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if an unsolved exception is thrown with no catch, Specimin will make the
 * synthetic type of that exception to extend RuntimeException.
 */
public class UncatchThrowTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "uncatchthrow",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(String)"});
  }
}
