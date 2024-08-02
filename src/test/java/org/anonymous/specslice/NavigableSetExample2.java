package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * Another test based on NavigableSets in java.util.Collections. This checks for a case where an
 * unsolved interface was incorrectly being inferred to be a class.
 */
public class NavigableSetExample2 {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "navigablesetexample2",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#unmodifiableNavigableSet(NavigableSet<T>)"});
  }
}
