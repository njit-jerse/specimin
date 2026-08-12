package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A lambda whose body ends in a bare {@code return;} is void, so the synthetic functional interface
 * for the method it is passed to must be a {@code Consumer} rather than a {@code Function}.
 *
 * <p>This is the converse of {@link LambdaNestedReturnTest}: both come from {@code
 * FullyQualifiedNameGenerator#getFQNsForLambdaType} deciding voidness with {@code stmt instanceof
 * ReturnStmt} over the lambda body's top-level statements only. That test covers a value-returning
 * lambda misclassified as void; this one covers a void lambda misclassified as value-returning,
 * because a {@code ReturnStmt} with no expression is counted as a return of a value. Today Specimin
 * emits {@code Function<? extends SyntheticTypeForItem, ?>} here and the output fails to compile
 * with "bad return type in lambda expression: missing return value".
 *
 * <p>A fix for the nested-return case that recurses for {@code ReturnStmt} without also checking
 * that the statement has an expression would leave this case broken, which is why it is tested
 * separately.
 */
public class LambdaBareReturnTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdabarereturn",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Wrapper)"});
  }
}
