package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if Specimin can solve mismatching in return types when a method is overriding by
 * using the error messages from javac.
 */
public class OverridingReturnTypeMatchingTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "overridingreturntypematching",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#get()"});
  }
}
