package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks that type corrections can be applied to inner classes. */
public class InnerClassTypeCorrectTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "innerclasstypecorrect",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
