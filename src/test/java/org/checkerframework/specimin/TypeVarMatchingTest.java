package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test makes sure that the use of a type variable in a synthetic file is consistent. */
public class TypeVarMatchingTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarmatching",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#get(E)"});
  }
}
