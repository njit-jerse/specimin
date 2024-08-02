package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks that arguments to unary expressions have appropriate types. */
public class UnaryExprTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "unaryexpr",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#test(Foo)"});
  }
}
