package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that when extending a JDK class with only constructors that take more than one
 * argument, at least one constructor gets preserved so that the result compiles.
 */
public class NoZeroArgCtorJDKTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "nozeroargctorjdk",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
