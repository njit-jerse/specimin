package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks that methods used in the body of a lambda in a target method are preserved. */
public class LambdaBodyTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdabody",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#toPos(Iterable<? extends SqlNode>)"});
  }
}
