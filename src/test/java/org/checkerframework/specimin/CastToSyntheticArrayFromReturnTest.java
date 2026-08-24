package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A cast to an array of a synthetic type, applied to something whose inferred type is not an array.
 * There is no pair of element types to relate here, so no supertype of {@code Baz} can make the
 * cast legal, and the requirement has to be met on the operand instead: {@code Foo#get}'s return
 * type becomes an unconstrained type variable, which every use site can instantiate as it needs.
 *
 * <p>Contrast {@code casttosynthetic}, where the same operand yields {@code Baz extends
 * GetReturnType}. Dropping the requirement here rather than relaxing the return type would leave
 * {@code Foo#get} returning a type unrelated to {@code Baz[]}, and the output would not compile.
 */
public class CastToSyntheticArrayFromReturnTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "casttosyntheticarrayfromreturn",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
