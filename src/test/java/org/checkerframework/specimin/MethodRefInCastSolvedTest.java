package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a method reference inside a cast still resolves to the real method it
 * refers to when that method is defined in the input, rather than being replaced by a synthetic
 * one: {@code Foo#mref()} is preserved, and {@code Foo#unused()} is pruned as usual.
 *
 * <p>The cast makes JavaParser throw rather than resolve the reference (see {@link
 * MethodRefInCastTest}), so Specimin has to find the referenced method from the scope's type
 * instead. This test covers that recovery in the case where the scope's type is solvable, which
 * {@link MethodRefInCastTest} does not reach: there, the scope's type is unsolved, so there is no
 * declaration to find and a synthetic method is generated instead.
 */
public class MethodRefInCastSolvedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "methodrefincastsolved",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
