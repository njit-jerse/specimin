package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * An array whose element type is a private generic type. A private type cannot be named from a
 * synthetic symbol in another package, so Specimin substitutes java.lang.Object -- and has to drop
 * the type arguments with it, since JLS 4.5 permits type arguments only on a generic type and
 * Object is not one. Keeping them would render {@code java.lang.Object<java.lang.String>[]}.
 */
public class PrivateGenericArrayTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "privategenericarray",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target()"});
  }
}
