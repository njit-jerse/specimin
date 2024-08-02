package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if SpecSlice can handle constraint type. */
public class ConstraintTypeTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "constraintType",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test(List<Baz>)"});
  }
}
