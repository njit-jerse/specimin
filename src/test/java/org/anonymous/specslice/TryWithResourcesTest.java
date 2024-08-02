package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that an unsolved type that's used in a try-with-resources context correctly
 * implements AutoCloseable.
 */
public class TryWithResourcesTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "trywithresources",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(OtherResource)"});
  }
}
