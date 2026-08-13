package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Calls to inherited synthetic methods through {@code this} and through no scope at all, where the
 * enclosing class's type variables must be left alone.
 *
 * <p>{@link TypeVarMatchingTest} covers the same situation for {@code super}. All three reach the
 * declaring type the same way -- the member is inherited, so the parameterization JLS 4.5.2
 * substitutes through is the one written in the {@code extends} clause, not one carried by a
 * receiver expression -- but only {@code super} was exercised. A rule that recognized the
 * substitution for {@code super} alone would declare {@code E} and {@code V} on these two methods,
 * shadowing {@code SimpleParent}'s own type parameters and severing the relationship between the
 * argument and return types.
 */
public class TypeVarFromCallerInheritedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarfromcallerinherited",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#get(E)"});
  }
}
