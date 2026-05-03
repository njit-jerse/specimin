package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that the synthetic interface generated for a lambda parameter has correct type
 * arguments based on the usages for a lambda parameter. For example, instead of Consumer<?>, it
 * should be Consumer<SomeType>.
 */
public class UnsolvedLambdaParameterUseTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedlambdaparameteruse",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
