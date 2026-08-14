package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The same unsolved call is the result of two lambdas with incompatible target types, and in one of
 * them it is reached through a return statement that is not the lambda's first. Constraining only
 * the first result expression would miss it entirely, because that lambda's first return is a
 * string literal, which constrains nothing.
 *
 * <p>No concrete return type works: {@code String} and {@code Payload} are unrelated, and a
 * synthetic subtype of {@code Payload} cannot also be a subtype of {@code String}, which is final
 * (JLS 8.1.1.2). The return type must therefore be an unconstrained type variable, which each
 * lambda instantiates to its own target type.
 */
public class LambdaResultUnsolvedLaterReturnTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdaresultunsolvedlaterreturn",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item, boolean)"});
  }
}
