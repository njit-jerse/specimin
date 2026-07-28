package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/** TODO. */
public class WrappedNodeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "wrappednode",
        new String[] {"com/example/MappingIterator.java"},
        new String[] {"com.example.MappingIterator#readAll()"});
  }
}
