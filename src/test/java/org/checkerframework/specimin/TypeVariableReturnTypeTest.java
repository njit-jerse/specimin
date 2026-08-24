package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A type variable (JLS 4.4) reaching FullyQualifiedNameGenerator#getFQNsForResolvedType. All
 * Specimin can say about one is the name its declaration gives it, which is in scope wherever the
 * type is reported, so the name reproduces the original type exactly.
 */
public class TypeVariableReturnTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevariablereturntype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target()"});
  }
}
