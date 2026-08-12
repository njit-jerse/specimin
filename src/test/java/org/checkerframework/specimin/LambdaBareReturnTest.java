package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A lambda whose body ends in a bare {@code return;} is void, so the synthetic functional interface
 * for the method it is passed to must be a {@code Consumer} rather than a {@code Function}.
 */
public class LambdaBareReturnTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdabarereturn",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Wrapper)"});
  }
}
