package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that when extending a non-JDK class with only constructors that take more than
 * zero arguments, at least one constructor gets preserved so that the result compiles.
 */
public class NoZeroArgCtorTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nozeroargctor",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
