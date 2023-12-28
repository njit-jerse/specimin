package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/** This test checks if Specimin can preserve annotations as needed */
public class PreserveAnnotationsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTest(
        "PreserveAnnotations",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#baz()"},
        new String[] {"src/test/resources/PreserveAnnotations/input/checker-qual-3.42.0.jar"});
  }
}
