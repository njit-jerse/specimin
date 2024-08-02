package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if SpecSlice will preserve target fields. */
public class TargetFieldTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "targetField",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#unsolvedField"});
  }
}
