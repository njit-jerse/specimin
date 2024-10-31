package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that we correctly preserve an inner class that only appears in an extends
 * clause.
 */
public class ExtendsInnerTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "extendsinner",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
