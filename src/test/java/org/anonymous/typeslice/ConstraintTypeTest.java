package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice can handle constraint type. */
public class ConstraintTypeTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "constraintType",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test(List<Baz>)"});
  }
}
