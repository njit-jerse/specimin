package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice can handle simple generic type expressions. */
public class GenericTypeTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "generictype",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple(T)"});
  }
}
