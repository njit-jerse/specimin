package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin properly handles a JavaParser bug where there is a clear method
 * declaration corresponding to a method call, when JavaParser throws a MethodAmbiguityException.
 */
public class PrimitiveArrayMethodOverloadsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "primitivearraymethodoverloads",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#hasNoDuplicates(byte[])"});
  }
}
