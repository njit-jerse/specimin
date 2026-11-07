package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin preserves the overload that javac actually selects when an
 * earlier-declared overload of the same arity has a parameter whose type is not on the source path.
 * See https://github.com/njit-jerse/specimin/issues/564; this is the variant without type
 * variables.
 *
 * <p>{@code Target}'s supertypes are all on the source path, and none is {@code View}, so {@code
 * Target} is not a subtype of {@code View} (JLS 4.10.2), and {@code clear(View)} is not applicable
 * (JLS 15.12.2.2). javac selects {@code clear(Target)}; the inapplicable overload cannot affect
 * that selection and is omitted.
 */
public class Issue564bTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue564b",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo(Target)"});
  }
}
