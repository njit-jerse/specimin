package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks that if Specimin will work properly where there are innerclasses in the input code
 */
public class InnerClass {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "innerclass",
        new String[] {"com/example/OuterFamily.java"},
        new String[] {"com.example.OuterFamily#getLastName()", "com.example.OuterFamily.InnerFamily#getLastName()"});
  }
}
