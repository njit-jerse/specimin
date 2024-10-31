package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if TypeSlice can preserve inner classes that contain used methods and remove
 * inner classes that do not.
 */
public class UsedMethodsInInnerClass {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "UsedMethodsInInnerClass",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
