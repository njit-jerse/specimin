package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The lambda in this test targets {@code Supplier<String>}, so by JLS 15.27.3 its result expression
 * sits in an assignment context whose target type is {@code String}. That constraint must reach the
 * unsolved call that produces the result: without it, Specimin gives {@code getPayload} a synthetic
 * placeholder return type, which cannot be converted to {@code String}, and the output does not
 * compile. See https://github.com/njit-jerse/specimin/issues/502.
 *
 * <p>The lambda's target type here comes from a local variable declaration, and the result
 * expression is reached through an explicit {@code return} in a block body.
 *
 * <p>The result type is {@code String}, which is final, so the constraint arrives as the
 * unconstrained-type-variable fallback rather than as {@code String} itself: nothing can be made a
 * subtype of a final class, so a placeholder return type is impossible and {@code <T> T} is what
 * satisfies the call site. That is the same output the equivalent non-lambda program produces for
 * {@code String s = item.getPayload();}, and the reason the constraint is applied in {@code
 * UnsolvedSymbolGenerator#addInformation} and not only during symbol generation.
 */
public class LambdaResultUnsolvedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdaresultunsolved",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
