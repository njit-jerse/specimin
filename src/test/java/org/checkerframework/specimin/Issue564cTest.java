package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin still preserves an overload whose parameter type is not on the
 * source path when the argument's type really is a subtype of it. It guards the fix for
 * https://github.com/njit-jerse/specimin/issues/564 (see {@link Issue564Test}), which rejects such
 * an overload only when every supertype of the argument's type is known. A fix that rejected every
 * overload with an unsolvable parameter type would drop {@code clear(View)} here.
 *
 * <p>{@code MyView} extends {@code View} and is not a {@code Target}, so only {@code clear(View)}
 * is applicable (JLS 15.12.2.2), and javac selects it. The inapplicable {@code clear(Target)}
 * cannot affect that selection and is omitted, along with {@code Target}.
 */
public class Issue564cTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue564c",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo(MyView)"});
  }
}
