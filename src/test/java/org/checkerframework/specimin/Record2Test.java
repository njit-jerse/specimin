package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks Specimin can process records. */
public class Record2Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "record2",
        new String[] {"com/example/Rectangle.java"},
        new String[] {"com.example.Rectangle#bar()"});
  }
}
