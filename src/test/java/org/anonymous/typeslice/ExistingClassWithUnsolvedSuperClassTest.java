package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks whether TypeSlice can create synthetic class for an existing class that extends
 * an unsolved class.
 */
public class ExistingClassWithUnsolvedSuperClassTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "existingclasswithunsolvedsuperclass",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
