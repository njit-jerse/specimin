package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that when extending a non-JDK class with only constructors that take more than
 * zero arguments (all of which are primitives rather than objects), at least one constructor gets
 * preserved so that the result compiles.
 */
public class NoZeroArgCtorIntTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nozeroargctorint",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
