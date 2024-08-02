package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if SpecSlice can remove unused bounds of type parameters in the declaration of a
 * class.
 */
public class UnusedTypeParameterBoundTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "unusedtypeparameterbound",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(T)"});
  }
}
