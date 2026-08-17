package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A variant of {@link LambdaResultUnsolvedTest} in which the lambda's target type comes from the
 * declared parameter type of the method it is passed to, rather than from a variable declaration.
 *
 * <p>The functional interface has more than one type argument, so this also pins down that the
 * result type is the *last* type argument of the (normalized) functional interface and not the
 * first: {@code BiFunction<String, Integer, Payload>} constrains the lambda's result to {@code
 * Payload}, not to {@code String}.
 */
public class LambdaResultUnsolvedArgumentTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdaresultunsolvedargument",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item)"});
  }
}
