package org.anonymous.typeslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that TypeSlice doesn't crash when the target method has a lambda parameter that
 * has type parameters.
 */
public class LambdaParamWithTypevarTest {
  @Test
  public void runTest() throws IOException {
    TypeSliceTestExecutor.runTestWithoutJarPaths(
        "lambdaparamwithtypevar",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
