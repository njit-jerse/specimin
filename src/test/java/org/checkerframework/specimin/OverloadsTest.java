package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks that Specimin respects Java's rules for overloading methods. */
public class OverloadsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "overloads",
        new String[] {"com/example/OverloadExamples.java"},
        new String[] {"com.example.OverloadExamples#test(MultipleMethods)"});
  }
}
