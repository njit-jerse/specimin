package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The enum counterpart of {@link NonExtendableTargetPrimitiveTest}. An enum declaration whose
 * constants have no class bodies is implicitly final (JLS 8.9), so nothing Specimin generates can
 * be made a subtype of it.
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
