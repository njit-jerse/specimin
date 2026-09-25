package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a lambda nested in an array initializer inside an array creation expression
 * targets the component type at its own depth (JLS 10.6), here Fn in a Fn[][], so that Fn keeps its
 * abstract method. It also guards against resolving a type that is not attached to the AST: the
 * array type here is written only on the array creation expression. See issue #567.
 */
public class LambdaSolvedInterfaceNestedArrayTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdasolvedinterfacenestedarray",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
