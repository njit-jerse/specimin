package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks if Specimin can handle constraint type.
 */
public class ConstraintTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "constraintType",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test(List<Baz>)"});
  }
}
