package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice can handle when the target class extends an inner class. */
public class InnerClassUsageTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "innerclassusage",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#test()"});
  }
}
