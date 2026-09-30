package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that an overload is treated as definitely applicable to a type-variable argument
 * when the type variable's bound is assignable to the parameter type, even if the bound has a
 * supertype that is not on the source path. Specimin should then prefer that overload over one that
 * is only possibly applicable.
 *
 * <p>The direct supertypes of {@code Y} are its bounds (JLS 4.10.2), so {@code Y} is a subtype of
 * {@code Sub} and therefore of {@code Target}, and {@code clear(Target)} is applicable (JLS
 * 15.12.2.2). {@code clear(View)} is applicable only if the unsolvable {@code Fragment} is a
 * subtype of {@code View}; even then, neither overload would be more specific than the other (JLS
 * 15.12.2.5) and the call would be ambiguous. Since the input compiles, javac must select {@code
 * clear(Target)}, and {@code clear(View)} is omitted along with its import. This is the
 * type-variable counterpart of {@link OverloadDefinitelyApplicableTest}.
 */
public class TypeVarBoundApplicableTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarboundapplicable",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo(Y)"});
  }
}
