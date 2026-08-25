package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that an array type with a generic element type survives the round trip through
 * FullyQualifiedNameSet.
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
