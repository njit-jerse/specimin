package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test ensures that Specimin will not miss unresolved types serving as type parameters within
 * another type.
 */
public class MapOfUnsolvedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "mapofunsolved",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
