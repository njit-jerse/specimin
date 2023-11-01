package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if Specimin will work properly where there are static nested classes in the
 * input code
 */
public class StaticNestedClass {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "staticnestedclass",
        new String[] {"com/example/OuterFamily.java"},
        new String[] {
          "com.example.OuterFamily#getLastName()",
          "com.example.OuterFamily.InnerFamily#getLastName()"
        });
  }
}
