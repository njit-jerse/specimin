package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/** This test checks if Specimin can work for methods that have type parameters themselves. */
public class TypeVarSimpleTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarsimple",
        new String[] {"com/example/TypeVarSimple.java"},
        new String[] {"com.example.TypeVarSimple#methodWithTypeParameter(ClassWithTypeParam<T>)"});
  }
}
