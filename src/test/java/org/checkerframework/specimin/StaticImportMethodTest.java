package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin can handle an unsolved and statically imported method. */
public class StaticImportMethodTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "staticimportmethod",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"});
  }
}
