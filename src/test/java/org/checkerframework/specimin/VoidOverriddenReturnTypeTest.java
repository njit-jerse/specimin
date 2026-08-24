package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A void return type (JLS 8.4.5) reaching FullyQualifiedNameGenerator#getFQNsForResolvedType, by
 * way of the synthetic superclass method that this one overrides: JLS 8.4.8.3 requires the two
 * return types to match, so Specimin has to name void to declare the overridden method. Like a
 * primitive, void is named by its keyword.
 */
public class VoidOverriddenReturnTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "voidoverriddenreturntype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target()"});
  }
}
