package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The array counterpart of {@link NonExtendableTargetPrimitiveTest}. No class declaration may name
 * an array type as a superclass, so a generated type cannot be made to fit this assignment and the
 * return type must fall back to an unconstrained type variable.
 */
public class NonExtendableTargetArrayTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nonextendabletargetarray",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
