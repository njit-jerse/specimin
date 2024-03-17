package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test makes sure that Specimin will not have type variables mismatched. */
public class TypeVariableMatchingTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevariablematching",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
