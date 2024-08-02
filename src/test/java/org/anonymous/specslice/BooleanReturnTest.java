package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if SpecSlice can correctly deduce that the return type of an unsolved
 * method that is used as an if or loop guard must be boolean.
 */
public class BooleanReturnTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "booleanreturn",
        new String[] {"com/example/Simple.java"},
        new String[] {
          "com.example.Simple#test()",
          "com.example.Simple#testFoo(Foo)",
          "com.example.Simple#testFoo2(Foo)"
        });
  }
}
