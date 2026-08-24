package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A synthetic method whose result is bounded by one use site is also called with an argument whose
 * type is a type variable of the caller.
 *
 * <p>The generated method must bind that type variable itself, since the caller's is not in scope
 * in the generated class, and its return type must stay independent of it: {@code U u =
 * item.get(arg);} and {@code Payload p = item.get(arg);} pass the same argument but want different
 * results, so a signature that returned the parameter's type could satisfy only one of them. Hence
 * {@code <U, T1> T1 get(U parameter0)}, with a second type variable for the result.
 *
 * <p>The hazard this guards against is a placeholder return type being mistaken for an unknown one.
 * Both carry a generated name, but a placeholder interposed on a bound stands for something
 * Specimin does know, and code that reads a generated name as "nothing is known here" will happily
 * swap it for a type variable already in the signature -- here, the caller's {@code U}, which ties
 * the result to the argument and does not compile.
 */
public class PlaceholderAndCallerTypeVariableTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "placeholderandcallertypevariable",
        new String[] {"com/example/Simple.java", "com/example/Payload.java"},
        new String[] {"com.example.Simple#bar(Item, U)"});
  }
}
