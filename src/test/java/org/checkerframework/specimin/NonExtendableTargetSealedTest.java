package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A sealed class as an assignment target. Only the classes named in {@code SealedBase}'s {@code
 * permits} clause may extend it (JLS 8.1.1.2), and a placeholder Specimin invents is never one of
 * them, so the return type of {@code get} cannot be made to fit this assignment and falls back to
 * an unconstrained type variable. Without that, the output declares {@code GetReturnType extends
 * SealedBase}, which does not compile because {@code GetReturnType} is not permitted.
 *
 * <p>The cast on the second line is what makes the sealed classification the deciding factor rather
 * than incidental. It keeps the placeholder return type alive until the assignment is examined; if
 * the assignment instead saw a return type that already existed, the repair for a conflict that no
 * supertype can fix would handle this program without consulting sealedness at all.
 */
public class NonExtendableTargetSealedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nonextendabletargetsealed",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
