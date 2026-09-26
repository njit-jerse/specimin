package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** This test checks that comments are preserved when using the OpenJML modularity model. */
public class OpenJMLKeepCommentsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runOpenJMLTestWithoutJarPaths(
        "openjml-keepcomments",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test()"});
  }
}
