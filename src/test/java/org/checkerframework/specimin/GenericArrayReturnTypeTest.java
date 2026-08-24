package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that an array type with a generic element type survives the round trip through
 * FullyQualifiedNameSet: the brackets ride on the erased FQN and the element's type arguments ride
 * beside them, so the bound this assignment imposes on the synthetic method has to be reassembled
 * into {@code Foo<?>[]} when it is written out.
 */
public class GenericArrayReturnTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "genericarrayreturntype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target()"});
  }
}
