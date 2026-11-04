package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that the abstract method of a functional interface on the source path is
 * preserved when a lambda targets it as an operand of a conditional expression. A lambda operand of
 * a poly reference conditional gets the conditional's own target type (JLS 15.25.3). See issue
 * #567.
 */
public class LambdaSolvedInterfaceConditionalTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdasolvedinterfaceconditional",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo(boolean)"});
  }
}
