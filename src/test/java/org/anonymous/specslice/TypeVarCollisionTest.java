package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks if SpecSlice can work for methods that have type parameters themselves. */
public class TypeVarCollisionTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "typevar-collision", new String[] {"org/Foo.java"}, new String[] {"org.Foo#useT(T)"});
  }
}
