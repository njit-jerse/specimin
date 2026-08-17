package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A block-bodied lambda has one result expression per return statement of its own, and the target
 * type constrains every one of them, not just the first (JLS 15.27.3 puts each result expression in
 * an assignment context). Both calls here must therefore be constrained by {@code
 * Supplier<String>}.
 *
 * <p>{@code String} is final, so neither call can be given a placeholder return type and both fall
 * back to an unconstrained type variable, which each call site instantiates to {@code String}.
 *
 * <p>The two calls deliberately have different receivers, so that each generated class declares
 * exactly one method; putting both on one receiver would make the test depend on the order in which
 * members of a generated class are emitted, which is not what it is trying to pin down.
 */
public class LambdaResultUnsolvedMultipleReturnsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdaresultunsolvedmultiplereturns",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Item, Other, boolean)"});
  }
}
