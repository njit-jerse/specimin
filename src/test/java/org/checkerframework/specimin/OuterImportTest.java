package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks that imports related to a used outer class declaration are preserved. */
public class OuterImportTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "outerimport",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
