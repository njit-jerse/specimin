package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks that TypeSlice can handle superclass with type variables. */
public class SuperClassWithGenericTypeTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "superclasswithgenerictype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
