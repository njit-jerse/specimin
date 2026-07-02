package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** This test checks if Specimin can handle the "this" expression. */
public class ThisClauseTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "thisclause",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple()"});
  }
}
