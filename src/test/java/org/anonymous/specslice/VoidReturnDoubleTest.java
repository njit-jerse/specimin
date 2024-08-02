package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** The test for TODO */
public class VoidReturnDoubleTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "voidreturndouble",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(MethodGen)"});
  }
}
