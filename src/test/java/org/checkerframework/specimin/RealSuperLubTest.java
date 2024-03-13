package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that Specimin can handle a case where the upper bound is a concrete unsolved
 * class that also has other constraints imposed on it by the structure of the target method.
 */
public class RealSuperLubTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "realsuperlub",
        new String[] {"com/example/MyNode.java"},
        new String[] {"com.example.MyNode#target(int)"});
  }
}
