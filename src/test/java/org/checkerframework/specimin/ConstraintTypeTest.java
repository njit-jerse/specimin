package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** This test checks if Specimin can handle constraint type. */
public class ConstraintTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "constraintType",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test(List<Baz>)"});
  }
}
