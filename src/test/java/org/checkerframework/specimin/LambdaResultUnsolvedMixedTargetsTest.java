package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link LambdaResultUnsolvedTwoTargetsTest}, but only one of the two target types is final.
 * That combination forces the unconstrained-type-variable fallback more sharply than two final
 * targets do, because here no concrete return type works at all:
 *
 * <ul>
 *   <li>{@code String} is not assignable to {@code Payload}, and {@code Payload} is not assignable
 *       to {@code String}, so neither target type can serve as the return type;
 *   <li>a synthetic subtype of {@code Payload} would satisfy the second lambda but cannot also be a
 *       subtype of {@code String}, which is final (JLS 8.1.1.2).
 * </ul>
 *
 * <p>The equivalent non-lambda program produces {@code <T> T getPayload()} today, and does so
 * regardless of which of the two declarations comes first. The non-final target is written first
 * here so that the two multi-target tests between them cover both orders.
 *
 * <p>Provisional on the same terms as {@link LambdaResultUnsolvedTwoTargetsTest}: both depend on
 * routing the lambda's result-type constraint through {@code
 * UnsolvedSymbolGenerator#addInformation} rather than through symbol generation alone.
 */
public class LambdaResultUnsolvedMixedTargetsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdaresultunsolvedmixedtargets",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
