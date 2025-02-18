package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that Specimin doesn't crash when an array is used as a type parameter. Based on
 * a crash we found when we ran on plume-util.
 */
public class ArrayTypeParamTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "arraytypeparam",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Object[])"});
  }
}
