package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that Specimin can create a synthetic signature for a method that is not defined
 * in the input but is used as a methodref.
 */
public class MethodRefUndefTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "methodrefundef",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(List<SqlNode>)"});
  }
}
