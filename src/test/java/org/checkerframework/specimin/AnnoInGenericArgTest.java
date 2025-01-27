package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that TargetMethodFinderVisitor can correctly find the target method when there
 * is an annotation in a generic type in the return type of the target in the original source.
 */
public class AnnoInGenericArgTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "annoingenericarg",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
