package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that a cast to a synthetic type constrains the cast's operand in no way, even when the
 * cast is the operand's only use site. Specimin chooses a synthetic type's supertypes, so it makes
 * {@code Baz} a subtype of whatever {@code get()} returns rather than forcing {@code get()} to
 * return something castable to {@code Baz}; the cast is then a legal downcast.
 *
 * <p>See {@link CastAndContextSyntheticTest} for the case where this matters for compilability --
 * there a second use site pins the return type, and constraining it would break that use site.
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
