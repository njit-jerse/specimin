package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if synthetic annotations used in different locations will contain the proper
 * ElementTypes in their {@code @Target} annotations
 */
public class SyntheticAnnotationTargetTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "syntheticannotationtarget",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#baz(U)"});
  }
}
