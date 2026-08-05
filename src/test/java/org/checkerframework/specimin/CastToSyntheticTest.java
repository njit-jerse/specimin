package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that a cast to a synthetic type does constrain the type inferred for the cast's operand.
 * This is the counterpart to {@link CastAndContextHarmlessTest} and {@link
 * CastAndContextInterfaceTest}: those cover the two targets that constrain nothing ({@code Object}
 * and an interface), while this covers a target that Specimin cannot resolve at all and so must
 * conservatively treat as constraining. Without that constraint, {@code get()} would be given a
 * freshly generated synthetic return type unrelated to {@code Baz}, and the cast would not compile.
 *
 * <p>{@code Object} is the right answer here because a cast from {@code Object} to any reference
 * type is a legal narrowing conversion, so it satisfies the cast no matter what Specimin later
 * decides {@code Baz} is.
 */
public class CastToSyntheticTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "casttosynthetic",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
