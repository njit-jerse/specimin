package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that Specimin correctly preserves the signatures of
 * methods used as methodrefs. Based on an example from Apache Calcite.
 */
public class MethodRefTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
            "methodref",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(List<SqlNode>)"});
  }
}
