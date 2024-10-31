package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** The test for TODO */
public class VoidReturnDoubleTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "voidreturndouble",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(MethodGen)"});
  }
}
