package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that the abstract method of a functional interface on the source path is
 * preserved when the only use of that interface is as the target type of a method reference. A
 * method reference, like a lambda, is only compatible with a functional interface type (JLS
 * 15.13.2), which must have exactly one abstract method (JLS 9.8). See issue #567.
 */
public class MethodRefSolvedInterfaceTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "methodrefsolvedinterface",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
