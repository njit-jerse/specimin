package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that if Specimin will produce compilable output if a target method references a
 * non-default constructor in the parent class, but also references a constructor in the target
 * class which contains a super() call.
 */
public class UnsolvedSuperConstructorTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedsuperconstructor",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()", "com.example.Foo#Foo()"});
  }
}
