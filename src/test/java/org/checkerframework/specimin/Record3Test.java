package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks Specimin can process records. */
public class Record3Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "record3",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
