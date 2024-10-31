package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks that TypeSlice preserves the right constructors in a constructor chain. */
public class ConstructorChainingTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "constructorchaining",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
