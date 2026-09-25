package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that when a lambda's target is a functional interface on the source path whose
 * abstract method is inherited from a superinterface, that inherited method is preserved: JLS 9.8
 * counts inherited abstract methods when deciding whether an interface is functional. The default
 * method in the subinterface is not abstract, so it does not affect that judgment and is removed.
 * See issue #567.
 */
public class LambdaSolvedInheritedInterfaceTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdasolvedinheritedinterface",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
