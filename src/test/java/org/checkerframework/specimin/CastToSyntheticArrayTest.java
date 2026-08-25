package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A cast to an array of a synthetic type. JLS 4.10.3 derives array subtyping from the element
 * types, and no class declaration can name an array type as a supertype, so the requirement that
 * makes the cast legal has to be recorded on {@code Baz} rather than on {@code Baz[]}: the expected
 * output has {@code Baz extends Bar}, which gives {@code Baz[] <: Bar[]}.
 *
 * <p>This is the array analogue of the {@code casttosynthetic} test.
 */
public class CastToSyntheticArrayTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "casttosyntheticarray",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Bar[])"});
  }
}
