package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The lambda's target result type is itself unsolved, so the type the result expression is
 * constrained to is one Specimin synthesizes rather than one it can look up. The constraint still
 * has to survive the trip from a set of candidate names back to the generated symbol; if it did
 * not, the lambda would be left with no left-hand side type at all.
 *
 * <p>{@code Foo} is a generated class and so is not final, which means the constraint applies
 * directly and no unconstrained-type-variable fallback is involved.
 */
public class LambdaResultUnsolvedSyntheticTargetTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdaresultunsolvedsynthetictarget",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
