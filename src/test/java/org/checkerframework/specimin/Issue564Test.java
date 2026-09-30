package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin preserves the overload that javac actually selects when another
 * overload of the same arity has a parameter whose type is not on the source path. The input is
 * reduced from the report in https://github.com/njit-jerse/specimin/issues/564.
 *
 * <p>The argument's type is the type variable {@code Y}, whose only supertypes are its bound {@code
 * Target<T>} and that bound's supertypes (JLS 4.10.2). All of those are on the source path, so
 * {@code Y} is not a subtype of the unsolvable {@code View}, and {@code clear(View)} is not
 * applicable (JLS 15.12.2.2). {@code Target<T>} is a subtype of {@code Target<?>} by containment
 * (JLS 4.5.1), so javac selects {@code clear(Target<?>)}. The inapplicable overload cannot affect
 * the selection and is omitted.
 *
 * <p>Unlike {@link Issue564bTest}, JavaParser cannot decide assignability here by itself, because
 * the argument is a type variable; the bound has to be consulted.
 */
public class Issue564Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue564",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo(Y)"});
  }
}
