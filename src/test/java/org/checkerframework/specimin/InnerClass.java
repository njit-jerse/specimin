package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that if Specimin will work properly where there are innerclasses in the input
 * code
 */
public class InnerClass {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "innerclass",
        new String[] {"com/example/OuterFamily.java"},
        new String[] {
          "com.example.OuterFamily#getLastName()",
          "com.example.OuterFamily.InnerFamily#getLastName()"
        });
  }
}
