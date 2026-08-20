package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin can handle calls to an enum's implicitly declared {@code values()}
 * and {@code valueOf(String)} methods (JLS 8.9.3), including on a top-level enum. Neither method
 * has an AST node of its own, so JavaParser reports the enum declaration itself as their AST;
 * Specimin used to crash (<a href="https://github.com/njit-jerse/specimin/issues/514">#514</a>)
 * when it tried to find the class-like element enclosing that AST, because a top-level enum has
 * none.
 *
 * <p>The enum constants are dropped from the expected output: no typing judgment about the target
 * depends on which constants exist, since the signatures of {@code values()} and {@code
 * valueOf(String)} do not mention them. The resulting {@code valueOf} call would fail at run time,
 * but Specimin preserves compile-time behavior only.
 */
public class ImplicitEnumMethodsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "implicitenummethods",
        new String[] {"p/UsesEnums.java"},
        new String[] {"p.UsesEnums#target()"});
  }
}
