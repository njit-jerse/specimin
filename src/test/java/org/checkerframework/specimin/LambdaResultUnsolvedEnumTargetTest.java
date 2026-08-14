package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The enum counterpart of {@link LambdaResultUnsolvedArrayTargetTest}. An enum declaration whose
 * constants have no class bodies is implicitly final (JLS 8.9), so nothing Specimin generates can
 * be made a subtype of it, and the result type must fall back to an unconstrained type variable
 * rather than to a synthetic subtype of the other lambda's target.
 *
 * <p>The enum is declared with no constants so that the test turns only on the declaration's kind.
 * Specimin prunes unreferenced members, so an enum carrying constants would make the expected
 * output depend on pruning behavior that has nothing to do with what is under test here.
 */
public class LambdaResultUnsolvedEnumTargetTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdaresultunsolvedenumtarget",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
