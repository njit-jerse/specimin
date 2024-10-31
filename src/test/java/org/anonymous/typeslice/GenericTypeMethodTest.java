package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test makes sure that TypeSlice will not modify existing class file in the input codebase
 * when dealing with a method with generic return type.
 */
public class GenericTypeMethodTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "generictypemethod",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
