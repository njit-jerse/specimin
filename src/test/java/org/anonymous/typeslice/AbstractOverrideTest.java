package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if a class extends an abstract class with a method that has to be present,
 * that method still gets deleted in both the superclass and the subclass if it's not used.
 */
public class AbstractOverrideTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "abstractoverride",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
