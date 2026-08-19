package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A non-extendable assignment target is not on its own a reason to weaken a synthetic method's
 * return type. Each call here has exactly one use site, so the type that site asks for is the
 * precise return type and it satisfies everything that has been asked; falling back to an
 * unconstrained type variable, as {@link NonExtendableTargetPrimitiveTest} and its siblings must,
 * would trade that precision for nothing.
 *
 * <p>Every kind of non-extendable target that {@code UnsolvedSymbolGenerator#isNonExtendableType}
 * recognizes from a resolved type appears once, so that broadening that method again cannot quietly
 * cost precision here.
 */
public class NonExtendableTargetNoConflictTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nonextendabletargetnoconflict",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
