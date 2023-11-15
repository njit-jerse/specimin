package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if Specimin will work for array types
 */
public class Issue_38Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unionType",
        new String[] {"com/example/Issue_38.java"},
        new String[] {"com.example.Issue_38#test()"});
  }
}