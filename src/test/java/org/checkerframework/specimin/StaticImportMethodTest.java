
package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks if Specimin can handle an unsolved and statically imported method.
 */
public class StaticImportMethodTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "staticimportmethod",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
