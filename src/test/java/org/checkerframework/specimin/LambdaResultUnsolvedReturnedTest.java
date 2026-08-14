package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A variant of {@link LambdaResultUnsolvedTest} in which the lambda is itself the operand of a
 * {@code return}, so its target type is the enclosing method's declared return type.
 *
 * <p>Two {@code return}s are nested here and they mean different things: the outer one is
 * constrained by {@code Simple#bar}'s declared type, and the inner one by the result type of the
 * functional interface that type names. Reading the lambda's target type has to walk out through
 * the outer {@code return} to find it.
 */
public class LambdaResultUnsolvedReturnedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdaresultunsolvedreturned",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
