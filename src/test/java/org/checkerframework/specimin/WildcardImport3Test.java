package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks the case where there are two wildcard imports, but the used class that one of
 * them refers to is actually in the input. In that case, the output should use that input class
 * rather than putting it in the wrong place.
 */
public class WildcardImport3Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "wildcardimport3",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
