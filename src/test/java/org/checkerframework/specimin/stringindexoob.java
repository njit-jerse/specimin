package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

public class stringindexoob {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "StringIndexOutOfBounds",
        new String[] {"com/example/BasicBeanDescription.java"},
        new String[] {"BasicBeanDescription#getConstructorsWithMode()"});
  }
}
