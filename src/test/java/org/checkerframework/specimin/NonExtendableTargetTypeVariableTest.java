package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The type-variable counterpart of {@link NonExtendableTargetPrimitiveTest}. A class declaration
 * may not name a type variable as a superclass or superinterface (JLS 8.1.4, 8.1.5), and the only
 * values assignable to an unbounded type variable are {@code null} and values already of that type,
 * so an unconstrained type variable is again the only return type that satisfies both assignments.
 */
public class NonExtendableTargetTypeVariableTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nonextendabletargettypevariable",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
