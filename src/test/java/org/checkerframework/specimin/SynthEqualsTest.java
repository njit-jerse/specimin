package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks that we correctly handle incomparable types resulting from equality tests. If one side
 * of such a test is synthetic, the resulting program will be non-compilable. */
public class SynthEqualsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "synthequals",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
