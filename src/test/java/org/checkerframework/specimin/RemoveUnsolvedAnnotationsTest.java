package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/** This test checks if Specimin can remove annotations as needed */
public class RemoveUnsolvedAnnotationsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTest(
        "removeunsolvedannotations",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#baz()"},
        new String[] {"src/test/resources/removeunsolvedannotations/input/checker-qual-3.42.0.jar"});
  }
}
