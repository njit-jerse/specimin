package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that the abstract method of a functional interface on the source path is
 * preserved when the only use of that interface is as the target type of a lambda. A lambda is only
 * compatible with a functional interface type (JLS 15.27.3), and an interface is functional only if
 * it has exactly one abstract method (JLS 9.8), so removing that method makes the lambda
 * uncompilable even though nothing calls it by name. See issue #567.
 */
public class LambdaSolvedInterfaceTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdasolvedinterface",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#makeConstructor()"});
  }
}
