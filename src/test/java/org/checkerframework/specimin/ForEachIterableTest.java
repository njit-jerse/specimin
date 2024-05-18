package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if a synthetic type is used as the iterable thing in a foreach loop,
 * Specimin does actually make it iterable.
 */
public class ForEachIterableTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "foreachiterable",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Baz)"});
  }
}
