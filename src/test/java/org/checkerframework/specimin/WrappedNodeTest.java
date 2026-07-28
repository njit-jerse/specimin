package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Test case for <a href="https://github.com/njit-jerse/specimin/issues/404">issue 404</a>. */
public class WrappedNodeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "wrappednode",
        new String[] {"com/example/MappingIterator.java"},
        new String[] {"com.example.MappingIterator#readAll()"});
  }
}
