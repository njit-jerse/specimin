package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that the abstract method of a functional interface on the source path is
 * preserved when a lambda targets it as an element of an array initializer. Each element of an
 * array initializer is in an assignment context whose target is the array's component type (JLS
 * 10.6). See issue #567.
 */
public class LambdaSolvedInterfaceArrayTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdasolvedinterfacearray",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
