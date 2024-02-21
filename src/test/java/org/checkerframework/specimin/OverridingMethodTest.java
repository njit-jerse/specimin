package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks that Specimin can handle method overriding correctly. */
public class OverridingMethodTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "overridingmethod",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(int, UnsolvedType)"});
  }
}
