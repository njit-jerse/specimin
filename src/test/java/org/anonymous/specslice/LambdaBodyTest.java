package org.anonymous.specslice;

import java.io.IOException;
import org.junit.Test;

/** This test checks that methods used in the body of a lambda in a target method are preserved. */
public class LambdaBodyTest {
  @Test
  public void runTest() throws IOException {
    SpecSliceTestExecutor.runTestWithoutJarPaths(
        "lambdabody",
        new String[] {
          "com/example/Simple.java",
          "com/example/sql/SqlNode.java",
          "com/example/sql/SqlParserPos.java",
          "com/example/util/Util.java",
          "com/example/util/Cast.java"
        },
        new String[] {"com.example.Simple#toPos(Iterable<? extends SqlNode>)"});
  }
}
