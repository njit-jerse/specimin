package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that a statically-imported, unsolved method used in the body of a lambda has an
 * appropriate/compatible synthetic method created for it. This version of the test adds another use
 * of the used method that has different typing constraints as a distractor, mimicking a problem
 * that showed up in CF-3850.
 */
public class LambdaBodyStaticUnsolved2Test {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "lambdabodystaticunsolved2",
        new String[] {
          "com/example/Simple.java",
          "com/example/sql/SqlNode.java",
          "com/example/sql/SqlParserPos.java",
          "com/example/util/Util.java",
          "com/example/AnotherClass.java"
        },
        new String[] {"com.example.Simple#toPos(Iterable<? extends SqlNode>)"});
  }
}
