package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if Specimin will preserve ancestors of types added in
 * MustImplementMethodsVisitor. It also checks to see if methods whose direct override is abstract,
 * but original definition is JDK are preserved. Test code derived from a modified, minimized
 * version of NullAway issue 103.
 */
public class Issue103Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue103",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
