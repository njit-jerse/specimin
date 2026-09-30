package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that an element of an array initializer is constrained to the array's component
 * type (JLS 10.6): the unsolved method must return a type assignable to Fn.
 */
public class UnsolvedArrayInitializerElementTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedarrayinitializerelement",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
