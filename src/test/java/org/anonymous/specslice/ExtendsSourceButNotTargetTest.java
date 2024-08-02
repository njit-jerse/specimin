package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that SpecSlice can correctly resolve a test case where there is a three-class
 * chain of extends clauses from the target file/class to another class that is provided as a source
 * file (but which is not a target). In other words, this test's goal is to make sure that SpecSlice
 * is handling files that are source-available but not targets correctly.
 */
public class ExtendsSourceButNotTargetTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "extendssourcebutnottarget",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
