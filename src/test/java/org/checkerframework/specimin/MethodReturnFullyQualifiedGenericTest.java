package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that Specimin includes the fully qualified class name in generics when
 * generating a synthetic method. For example, if a return type is Bar<com.foo.Foo>, the actual
 * synthetic return type should be com.qualified.Bar<com.foo.Foo>. This test encodes Specimin's
 * current behavior, which doesn't produce compilable output. If you break this test, it might 
 * not be a bad thing.
 */
public class MethodReturnFullyQualifiedGenericTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "methodreturnfullyqualifiedgeneric",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#alreadyQualified()"});
  }
}
