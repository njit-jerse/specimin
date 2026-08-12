package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A lambda whose only {@code return} statements are nested inside an {@code if} must still be
 * treated as returning a value, so that the synthetic functional interface for the method it is
 * passed to is a {@code Function} rather than a {@code Consumer}.
 *
 * <p>This is one of the two causes of the non-compiling output reported in <a
 * href="https://github.com/njit-jerse/specimin/issues/442">issue #442</a>. In that issue's input,
 * two lambdas are passed to two synthetic methods in the same chained call: {@code skipUntil}'s
 * lambda ends in a top-level {@code return} and correctly gets a {@code Function}, while {@code
 * map}'s lambda returns from inside an if/else and incorrectly gets a {@code Consumer}. The output
 * then fails to compile with "incompatible types: unexpected return value".
 *
 * <p>The cause is in {@code FullyQualifiedNameGenerator#getFQNsForLambdaType}, which decides
 * voidness by scanning only the top-level statements of the lambda's block body for a {@code
 * ReturnStmt}. See {@link LambdaBareReturnTest} for the converse defect at the same site.
 */
public class LambdaNestedReturnTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdanestedreturn",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Wrapper)"});
  }
}
