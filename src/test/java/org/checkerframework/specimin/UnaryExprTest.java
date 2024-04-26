package org.checkerframework.specimin;

import org.junit.Test;

import java.io.IOException;

/** This test checks that arguments to unary expressions have appropriate types. */
public class UnaryExprTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unaryexpr",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test(Foo)"});
  }
}
