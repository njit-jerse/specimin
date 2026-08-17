package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The same unsolved call supplies the result of two lambdas with different final target types. No
 * single concrete return type satisfies both, so the return type has to become an unconstrained
 * type variable, which each call site then infers separately (JLS 18.5.2 propagates the target type
 * of a lambda's result expression into the inference for a generic method invocation there).
 *
 * <p>This is what the equivalent non-lambda program already does: {@code String s =
 * item.getPayload(); Integer i = item.getPayload();} produces {@code <T> T getPayload()} today. The
 * fallback lives in {@code UnsolvedSymbolGenerator#addInformation}, which recognizes a final
 * left-hand side and calls {@code setUnconstrainedReturnType}, so this test is what forces the
 * lambda case to route its constraint through that code and not only through generation.
 *
 * <p>This test is provisional. If routing the constraint through {@code addInformation} turns out
 * to be disproportionately involved, it should be removed and refiled as its own issue; the
 * generation-only fix alone still makes the single-target cases (the other {@code
 * LambdaResultUnsolved*} tests) compile, which is the bug that
 * https://github.com/njit-jerse/specimin/issues/502 reports.
 */
public class LambdaResultUnsolvedTwoTargetsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdaresultunsolvedtwotargets",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
