package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks whether Specimin can create synthetic class for an existing class that extends
 * an unsolved class.
 */
public class ExistingClassWithUnsolvedSuperClassTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "existingclasswithunsolvedsuperclass",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
