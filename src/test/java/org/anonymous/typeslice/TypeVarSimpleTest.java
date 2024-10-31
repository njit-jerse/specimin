package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if TypeSlice can work for methods that have type parameters themselves. */
public class TypeVarSimpleTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "typevarsimple",
        new String[] {"com/example/TypeVarSimple.java"},
        new String[] {"com.example.TypeVarSimple#methodWithTypeParameter(ClassWithTypeParam<T>)"});
  }
}
