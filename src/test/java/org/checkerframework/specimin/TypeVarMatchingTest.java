package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that the names of type variables of a synthetic class match the names of those
 * type variables when the class is used.
 */
public class TypeVarMatchingTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarmatching",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#get(E)"});
  }
}
