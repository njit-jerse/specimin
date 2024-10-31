package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that we correctly preserve an inner class that only appears in an extends
 * clause. This variant also has type parameters on the inner class.
 */
public class ExtendsInner2Test {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "extendsinner2",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
