package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks whether SpecSlice can create synthetic class for an existing class that extends
 * an unsolved class.
 */
public class ExistingClassWithUnsolvedSuperClassTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "existingclasswithunsolvedsuperclass",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
