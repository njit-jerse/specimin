package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that, in case the target method has an override annotation and the parent class
 * is missing, then a synthetic version of the overriden method in the parent class will be created.
 */
public class UnsolvedOverrideAnnotationTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "unsolvedoverrideannotation",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
