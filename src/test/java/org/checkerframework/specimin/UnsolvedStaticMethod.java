package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin will work for input files that contain unsolved static methods. */
public class UnsolvedStaticMethod {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedstaticmethod",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
