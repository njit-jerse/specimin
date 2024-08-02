package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that when SpecSlice targets a constructor that assigns a final field, it doesn't
 * introduce an incorrect and unneeded assignment to that field.
 */
public class FinalFieldAssignTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "finalfieldassign",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple(int)"});
  }
}
