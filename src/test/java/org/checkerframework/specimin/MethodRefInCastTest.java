package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin can handle a method reference that appears as the operand of a
 * cast to a functional interface, and whose scope has an unsolved type: it must synthesize the
 * referenced method in the synthetic class for the scope's type, using the cast's functional
 * interface to determine the method's parameter types and voidness.
 *
 * <p>This is a regression test for https://github.com/njit-jerse/specimin/issues/479. JavaParser's
 * {@code MethodReferenceExprContext#inferArgumentTypes} only knows how to find the target
 * functional interface when the method reference's parent is a method call, an object creation, a
 * variable declarator, or a return statement; for any other parent, including a cast, it throws an
 * {@code UnsupportedOperationException}.
 */
public class MethodRefInCastTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "methodrefincast",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
