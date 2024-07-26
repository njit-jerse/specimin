package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that, given an @Override method with a throws clause in a child class, Specimin
 * will preserve the exceptions in its synthetic parent definition
 */
public class InheritMethodExceptionTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "inheritmethodexception",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
