package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The type-variable counterpart of {@link NonExtendableTargetPrimitiveTest}. A class declaration
 * may not name a type variable as a superclass or superinterface (JLS 8.1.4, 8.1.5), and the only
 * values assignable to an unbounded type variable are {@code null} and values already of that type,
 * so an unconstrained type variable is again the only return type that satisfies both assignments.
 *
 * <p>The target method's {@code U} is not in scope in the generated {@code Item}, so the generated
 * method binds a type variable of its own; the name it gets is whichever one is free, which is why
 * the expected output says {@code T1} rather than {@code T}.
 *
 * <p>Only this assignment form is fixed. The lambda form of the same program -- {@code Supplier<U>
 * s = () -> item.get();} -- still does not compile, because the constraint is unnameable at symbol
 * generation time; see issue 509.
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
