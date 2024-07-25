package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if a functional interface has an unused method, it is removed, and if a
 * synthetic functional interface method is generated, it is preserved.
 */
public class UnusedFuncInterfaceMethodTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unusedfuncinterfacemethod",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
