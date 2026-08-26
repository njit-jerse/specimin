package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a java.lang type named in the body of an anonymous class whose supertype is
 * unsolvable is recognized as such, rather than being guessed into the enclosing package. The
 * override's parameter type must be identical to the overridden method's (JLS 8.4.2), so the
 * synthetic supertype has to declare {@code started(java.lang.String)}. This is the same bug as
 * {@link JavaLangExceptionTest} (issue 525), on a parameter type rather than a throws clause.
 */
public class JavaLangParamTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "javalangparamtype",
        new String[] {"example/Target.java"},
        new String[] {"example.Target#target()"});
  }
}
