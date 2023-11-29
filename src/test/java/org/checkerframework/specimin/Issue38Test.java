package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin will work for Union types */
public class Issue38Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue38",
        new String[] {"com/example/Issue38.java"},
        new String[] {"com.example.Issue38#test()"});
  }
}
