package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The constructor counterpart of {@link TypeVarFromCallerSubstitutedTest}: a calling method's type
 * variable that must be left alone in a synthetic constructor's signature.
 *
 * <p>{@code new Box<T>(src)} supplies the class's type argument explicitly, so JLS 4.5.2 applies to
 * the constructor exactly as it does to a method of a parameterized type, and the caller's {@code
 * T} reaches the parameter through the class's type parameter rather than by an out-of-scope name.
 * Rewriting it to a constructor-level type variable would compile but would sever the returned
 * {@code Box}'s element type from the argument's.
 */
public class TypeVarFromCallerCtorSubstitutedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarfromcallerctorsubstituted",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Src<T>)"});
  }
}
