package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a bound method reference whose receiver is a pattern variable (JLS 14.30.1)
 * of an unsolved type puts the method on the pattern's type. JavaParser parses the scope {@code f}
 * of {@code f::baz} as a type expression, and before the fix for
 * https://github.com/njit-jerse/specimin/issues/563 the pattern variable's declared type could not
 * be found, so Specimin read {@code f} as a type name and invented a class {@code com.example.f} to
 * hold {@code baz}, leaving {@code Foo} without it.
 */
public class PatternVariableMethodRefTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "patternvariablemethodref",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Object)"});
  }
}
