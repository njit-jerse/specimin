package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a constructor reference to an unsolved type generates a constructor in that
 * type's synthetic class.
 *
 * <p>JavaParser refuses to resolve any constructor reference at all -- {@code
 * MethodReferenceExprContext#solveMethod} throws "Constructor calls not yet resolvable" for {@code
 * ::new} regardless of context -- so before the fix for
 * https://github.com/njit-jerse/specimin/issues/479, which taught Specimin to recover from that
 * exception, this input crashed and Specimin's code for generating a constructor from a constructor
 * reference was unreachable. That code marked the generated constructor {@code static}, which does
 * not compile; this test pins the corrected output.
 */
public class CtorRefUnsolvedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "ctorrefunsolved",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
