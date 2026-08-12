package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A lambda whose only {@code return} statements are nested inside an {@code if} must still be
 * treated as returning a value, so that the synthetic functional interface for the method it is
 * passed to is a {@code Function} rather than a {@code Consumer}.
 */
public class LambdaNestedReturnTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdanestedreturn",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Wrapper)"});
  }
}
