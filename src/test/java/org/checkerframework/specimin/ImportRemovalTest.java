package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks that when a class is removed, all of its imports are, too. */
public class ImportRemovalTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "importremoval",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
