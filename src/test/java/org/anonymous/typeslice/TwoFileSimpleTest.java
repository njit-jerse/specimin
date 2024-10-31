package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that two simple Java files, one of which has a dependency on the other, are
 * reduced correctly. There is one extraneous constructor in the file that doesn't have the target
 * method that ought to be removed.
 */
public class TwoFileSimpleTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "twofilesimple",
        new String[] {"com/example/Foo.java", "com/example/Baz.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
