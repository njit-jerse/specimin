package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/**
 * This test checks the case where there are two wildcard imports, but the used class
 * that one of them refers to is actually in the input. In that case, the output should
 * use that input class rather than putting it in the wrong place.
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
