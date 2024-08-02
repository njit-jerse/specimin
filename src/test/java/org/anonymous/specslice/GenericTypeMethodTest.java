package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test makes sure that SpecSlice will not modify existing class file in the input codebase
 * when dealing with a method with generic return type.
 */
public class GenericTypeMethodTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "generictypemethod",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
