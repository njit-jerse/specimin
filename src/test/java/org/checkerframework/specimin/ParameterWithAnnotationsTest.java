package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that Specimin can properly ignore annotations in the parameters of a target
 * method while comparing method signatures.
 */
public class ParameterWithAnnotationsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "parameterwithannotations",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(int, UnsolvedType)"});
  }
}
