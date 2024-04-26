package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that interaction between JavaTypeCorrect and adding unsolved methods to unsolved
 * classes doesn't cause a method to be lost. Based on a historic bug.
 */
public class UnsolvedMethodTypeCorrectTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedmethodtypecorrect",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
