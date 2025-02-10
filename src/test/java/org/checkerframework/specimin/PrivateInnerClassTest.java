package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * Tests that when a private inner class is the only actual argument used for a method that _should_
 * be generic, Specimin correctly makes it generic; doing otherwise would make Specimin's output
 * non-compilable.
 */
public class PrivateInnerClassTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "privateinnerclass",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
