package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if Specimin can remove unused bounds of type parameters in the declaration of a
 * class.
 */
public class UnusedTypeParameterBoundTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unusedtypeparameterbound",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
