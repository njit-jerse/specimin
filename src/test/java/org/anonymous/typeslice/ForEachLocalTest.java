package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** Test for <a href="https://github.com/kelloggm/specSlice/issues/165">#165</a>. */
public class ForEachLocalTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "foreachlocal",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
