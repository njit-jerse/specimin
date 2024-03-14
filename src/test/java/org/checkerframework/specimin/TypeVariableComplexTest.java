package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin can handle type variables used in a complex inheritance chain. */
public class TypeVariableComplexTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevariablecomplex",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
