package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * When a lambda's target type is a fully known functional interface, the return type of that
 * interface's method constrains the lambda's return expression. Here {@code supplier} is declared
 * {@code Supplier<String>}, so the unsolved {@code getPayload()} must be given the return type
 * {@code String}.
 *
 * <p>Specimin instead invents a {@code GetPayloadReturnType} for it, and the output fails to
 * compile with "GetPayloadReturnType cannot be converted to String".
 *
 * <p>The constraint is imposed by the {@code LambdaExpr} branch of {@code
 * UnsolvedSymbolGenerator#addInformation}, which relates the functional interface's return type to
 * the type of the lambda's return expression. That branch bails out early when {@code
 * Resolver#calculateResolvedType} cannot resolve the lambda, which is exactly what happens when the
 * lambda's return expression is itself unsolved — so the constraint is dropped in precisely the
 * case where it was needed. The defect is not specific to arity: it reproduces the same way for a
 * one-argument lambda targeting {@code Function<String, Integer>}.
 */
public class LambdaReturnTypeConstraintTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdareturntypeconstraint",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Item)"});
  }
}
