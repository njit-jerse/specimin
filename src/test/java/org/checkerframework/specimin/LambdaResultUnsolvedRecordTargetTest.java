package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The record counterpart of {@link LambdaResultUnsolvedArrayTargetTest}. A record declaration is
 * implicitly final (JLS 8.10), so nothing Specimin generates can be made a subtype of it, and the
 * result type must fall back to an unconstrained type variable.
 *
 * <p>The record is declared with no components for the reason given in {@link
 * LambdaResultUnsolvedEnumTargetTest}: Specimin prunes unreferenced record components (see the
 * {@code records} fixture), so components would make the expected output depend on pruning behavior
 * unrelated to what is under test.
 */
public class LambdaResultUnsolvedRecordTargetTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdaresultunsolvedrecordtarget",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
