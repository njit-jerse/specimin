package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks for a crash when targeting a field in the cf-691 example from the JDK. */
public class NavigableSetFieldExample {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "navigablesetfieldexample",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple.UnmodifiableNavigableSet#EMPTY_NAVIGABLE_SET"});
  }
}
