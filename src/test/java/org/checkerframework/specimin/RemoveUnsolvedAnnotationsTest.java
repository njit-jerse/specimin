package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin can remove annotations as needed */
public class RemoveUnsolvedAnnotationsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTest(
        "removeunsolvedannotations",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#baz()"},
        new String[] {"src/test/resources/shared/checker-qual-3.42.0.jar"});
  }
}
