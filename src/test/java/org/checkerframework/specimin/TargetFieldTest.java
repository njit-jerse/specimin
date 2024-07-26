package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin will preserve target fields. */
public class TargetFieldTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "targetField",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#unsolvedField"});
  }
}
