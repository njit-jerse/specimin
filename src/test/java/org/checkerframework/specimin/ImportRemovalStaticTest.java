package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks that static imports of unused members are removed. */
public class ImportRemovalStaticTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "importremovalstatic",
        new String[] {"com/example/Simple.java", "org/example/Preconditions.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
