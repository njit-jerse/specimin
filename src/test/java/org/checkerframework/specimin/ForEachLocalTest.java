package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** Test for <a href="https://github.com/kelloggm/specimin/issues/165">#165</a>. */
public class ForEachLocalTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "foreachlocal",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
