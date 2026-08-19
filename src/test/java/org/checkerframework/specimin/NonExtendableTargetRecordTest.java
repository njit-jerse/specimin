package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The record counterpart of {@link NonExtendableTargetPrimitiveTest}. A record declaration is
 * implicitly final (JLS 8.10), so nothing Specimin generates can be made a subtype of it.
 *
 * <p>The record is declared with no components for the reason {@link NonExtendableTargetEnumTest}
 * gives for its enum having no constants.
 */
public class NonExtendableTargetRecordTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nonextendabletargetrecord",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
