package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that extra packages aren't extraneously created, based on a bug I encountered
 * while minimizing java.util.Collections in the JDK.
 */
public class NavigableSetExample {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "navigablesetexample",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#emptyNavigableSet()"});
  }
}
