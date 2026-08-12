package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that when a target method's parameter has both an unsolvable annotation and an
 * unsolvable type, Specimin generates a synthetic class for the parameter's type as well as for the
 * annotation.
 *
 * <p>This is a regression test for https://github.com/njit-jerse/specimin/issues/495. The
 * annotation was a red herring: the bug was that {@code javax.ws.rs.core.UriInfo} was treated as
 * part of the JDK (because of a {@code startsWith("javax.")} heuristic) and so was never
 * synthesized, while the annotation was synthesized on a code path that does not consult that
 * heuristic. The fix for https://github.com/njit-jerse/specimin/issues/494 (PR #498) replaced the
 * heuristic with the real package list of the running JDK, which also fixed this input.
 */
public class AnnotatedParamTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTest(
        "annotatedparamtype",
        new String[] {"org/example/Target.java"},
        new String[] {"org.example.Target#target(UriInfo)"},
        "cf",
        new String[] {});
  }
}
