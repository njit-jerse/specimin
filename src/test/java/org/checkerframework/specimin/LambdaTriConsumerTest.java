package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that we create a synthetic 3-typevar top type for functions that take 3
 * parameters and don't return a value when a lambda that takes three parameters and doesn't return
 * a value is passed to a function in a synthetic class.
 *
 * <p>The three lambda parameters are used only in a string concatenation, which constrains their
 * types in no way (see {@link ConcatOfUnsolvedTest}), so each gets a placeholder type. This test
 * previously expected {@code ? extends java.lang.String} for all three, which was an instance of
 * the bug that ConcatOfUnsolvedTest covers: a single concatenation is not evidence that an operand
 * is a String. The placeholder types are less minimal but not less compilable, and collapsing
 * unconstrained placeholders is a separate issue -- see {@link CastAndContextHarmlessTest}.
 */
public class LambdaTriConsumerTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdatriconsumer",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(LambdaUser)"});
  }
}
