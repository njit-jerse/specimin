package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin can recognize classes belonging the original codebase correctly. */
public class ClassIsInOriginalCodebaseTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "classinoriginalcodebase",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo(SomeClass)"});
  }
}
