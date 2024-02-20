package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks whether Specimin can handle a method from a superclass that is implicitly
 * invoked.
 */
public class ImplicitSuperCallTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "implicitsupercall",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
