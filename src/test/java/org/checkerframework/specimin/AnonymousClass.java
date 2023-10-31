package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks that if Specimin will work if there is an anonymous class inside the target method
 */
public class AnonymousClass {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "anonymousclass",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#testAnonymous()"});
  }
}
