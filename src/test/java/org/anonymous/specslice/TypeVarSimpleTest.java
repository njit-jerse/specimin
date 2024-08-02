package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if SpecSlice can work for methods that have type parameters themselves. */
public class TypeVarSimpleTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "typevarsimple",
        new String[] {"com/example/TypeVarSimple.java"},
        new String[] {"com.example.TypeVarSimple#methodWithTypeParameter(ClassWithTypeParam<T>)"});
  }
}
