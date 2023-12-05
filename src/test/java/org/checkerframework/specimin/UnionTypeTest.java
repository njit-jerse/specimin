package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/** This test checks if Specimin can work when there is an unsolved union type. */
public class UnionTypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "uniontype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
