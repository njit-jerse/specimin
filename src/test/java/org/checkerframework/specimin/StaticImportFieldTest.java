package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin can handle an unsolved and statically imported field. */
public class StaticImportFieldTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "staticimportfield",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"});
  }
}
