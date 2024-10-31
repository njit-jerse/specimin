package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test is exactly like {@link AbstractOverrideTest}, but with an interface instead of an
 * abstract class.
 */
public class AbstractOverride2Test {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "abstractoverride2",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
