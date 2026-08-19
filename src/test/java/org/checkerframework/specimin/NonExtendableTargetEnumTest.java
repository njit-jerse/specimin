package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The enum counterpart of {@link NonExtendableTargetPrimitiveTest}. An enum declaration whose
 * constants have no class bodies is implicitly final (JLS 8.9), so nothing Specimin generates can
 * be made a subtype of it.
 *
 * <p>The enum is declared with no constants so that the test turns only on the declaration's kind:
 * Specimin prunes unreferenced members, so constants would make the expected output depend on
 * pruning behavior that has nothing to do with what is under test.
 */
public class NonExtendableTargetEnumTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nonextendabletargetenum",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
