package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that a simple Java file with a field declaration with more than one variable
 * declarator does not cause Specimin to crash.
 */
public class MultipleVariableDeclaratorsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "multiplevariabledeclarators",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
