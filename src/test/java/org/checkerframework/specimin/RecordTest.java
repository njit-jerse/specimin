package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks Specimin can process records. */
public class RecordTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "record",
        new String[] {"com/example/Rectangle.java"},
        new String[] {"com.example.Rectangle#rotate()"});
  }
}
