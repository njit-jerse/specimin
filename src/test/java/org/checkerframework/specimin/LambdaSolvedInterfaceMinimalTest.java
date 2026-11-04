package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test is a minimized version of {@link LambdaSolvedInterfaceTest}: a lambda whose target is a
 * non-generic functional interface on the source path, in a local variable initializer. The
 * interface's abstract method must be preserved (JLS 9.8, 15.27.3). See issue #567.
 */
public class LambdaSolvedInterfaceMinimalTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdasolvedinterfaceminimal",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
