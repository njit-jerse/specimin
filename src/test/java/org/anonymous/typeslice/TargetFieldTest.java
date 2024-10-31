package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice will preserve target fields. */
public class TargetFieldTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "targetField",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#unsolvedField"});
  }
}
