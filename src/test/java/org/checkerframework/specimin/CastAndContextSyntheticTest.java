package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link CastAndContextHarmlessTest}, but the cast target is a synthetic type rather than
 * {@code Object}. Specimin chooses what a synthetic type's supertypes are, so rather than
 * constraining {@code get()}'s return type to make the cast legal -- which would also degrade the
 * {@code Bar} that the assignment on the previous line establishes -- it makes {@code Baz} a
 * subtype of that return type, turning the cast into a legal downcast. This is the same treatment
 * {@code instanceof} already receives, and for the same reason: JLS 15.20.2 gives the two
 * constructs the same legality condition.
 */
public class CastAndContextSyntheticTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "castandcontextsynthetic",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
