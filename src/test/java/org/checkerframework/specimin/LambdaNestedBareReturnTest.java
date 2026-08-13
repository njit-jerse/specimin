package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A {@code return} statement inside a lambda nested in another lambda's body belongs to the inner
 * lambda, and must not be used to classify the outer one.
 *
 * <p>Here the outer lambda returns a value, so {@code map} must take a {@code Function}, but the
 * inner {@code Runnable} ends in a bare {@code return;}. A recursive search for the outer lambda's
 * returns reaches the inner lambda's bare return <em>first</em>, since it appears earlier in the
 * body. Attributing it to the outer lambda would classify the outer lambda as void, giving {@code
 * map} a {@code Consumer} parameter and producing output that fails to compile.
 */
public class LambdaNestedBareReturnTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "lambdanestedbarereturn",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Wrapper)"});
  }
}
