package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if Specimin can remove unused bounds of type parameters in the declaration of a
 * class. The only difference between this test and the other of the same name is that in this test,
 * the classes used in the bounds are not in the input program.
 */
public class UnusedTypeParameterBound2Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unusedtypeparameterbound2",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(T)"});
  }
}
