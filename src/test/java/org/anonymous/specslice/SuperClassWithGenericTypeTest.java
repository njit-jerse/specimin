package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks that SpecSlice can handle superclass with type variables. */
public class SuperClassWithGenericTypeTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "superclasswithgenerictype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
