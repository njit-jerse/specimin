package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A primitive type reaching FullyQualifiedNameGenerator#getFQNsForResolvedType. A primitive is
 * named by its keyword (JLS 4.2), which is also how Specimin names it, so the assignment bounds the
 * synthetic method's return type at {@code int}.
 */
public class PrimitiveReturnTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "primitivereturntype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target()"});
  }
}
