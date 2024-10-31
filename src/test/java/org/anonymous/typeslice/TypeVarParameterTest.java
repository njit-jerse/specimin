package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if TypeSlice can handle a method call with a parameter of generic type, such as
 * foo(T input).
 */
public class TypeVarParameterTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "typevarparameter",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
