package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if TypeSlice will work properly where there are static nested classes in
 * the input code
 */
public class StaticNestedClass {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "staticnestedclass",
        new String[] {"com/example/OuterFamily.java"},
        new String[] {
          "com.example.OuterFamily#getLastName()",
          "com.example.OuterFamily.InnerFamily#getLastName()"
        });
  }
}
