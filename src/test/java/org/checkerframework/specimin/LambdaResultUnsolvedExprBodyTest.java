package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The expression-body counterpart of {@link LambdaResultUnsolvedTest}. An expression-bodied lambda
 * has no {@code ReturnStmt} at all -- the result expression's parent is the {@code LambdaExpr}
 * itself -- so the target type must reach it by a different route, even though JLS 15.27.3 puts it
 * in the same assignment context.
 *
 * <p>The target type is also a non-final class here rather than {@code String}, so the result type
 * appears in the output as itself rather than through the unconstrained-type-variable fallback that
 * a final target type triggers.
 */
public class LambdaResultUnsolvedExprBodyTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdaresultunsolvedexprbody",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
