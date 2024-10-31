package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that TypeSlice preserves the right constructors in a constructor chain, but
 * doesn't preserve other things in those constructors (or their dependencies, etc).
 */
public class ConstructorChaining2Test {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "constructorchaining2",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
