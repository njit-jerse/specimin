package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a cast supplies the target type of a lambda (JLS 15.16), and so constrains
 * the lambda's result expressions (JLS 15.27.3): the unsolved method called in the lambda body must
 * return a type assignable to Fn. Fn keeps no methods, since no lambda targets it.
 */
public class LambdaResultUnsolvedCastTargetTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdaresultunsolvedcasttarget",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
