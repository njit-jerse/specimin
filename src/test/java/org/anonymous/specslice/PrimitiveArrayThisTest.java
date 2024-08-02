package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that instance fields with primitive array types do not cause a crash in
 * SpecSlice.
 */
public class PrimitiveArrayThisTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "primitivearraythis",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
