package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that methods used in the target that are only called with null arguments are
 * preserved correctly.
 */
public class NullArgTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nullarg",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Baz)"});
  }
}
