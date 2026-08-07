package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that Specimin produces a compilable output when a synthetic type is used both as the
 * supertype of another synthetic type (via a cast) and as the return type of a synthetic method
 * whose result is assigned to a variable whose type is a final class. The latter makes the method's
 * return type unconstrained, and the supertype relationship must not be dragged along when that
 * happens: an {@code extends} clause naming a method type variable is not in scope at the class
 * declaration, and so does not compile.
 */
public class DanglingUnconstrainedReturnTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "danglingunconstrainedreturntype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
