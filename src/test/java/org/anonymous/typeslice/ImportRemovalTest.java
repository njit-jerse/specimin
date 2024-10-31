package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks that when a class is removed, all of its imports are, too. */
public class ImportRemovalTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "importremoval",
        new String[] {"com/example/Simple.java", "com/example/NonsensicalList.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
