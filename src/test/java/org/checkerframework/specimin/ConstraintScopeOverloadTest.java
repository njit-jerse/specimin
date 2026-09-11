package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that when a call whose scope's type is a lambda constraint type invokes an
 * overloaded method, Specimin resolves it to the most specific applicable overload, as JLS
 * 15.12.2.5 requires. Preserving the other, less specific overload instead gives the call the wrong
 * return type, which here makes the target method's return statement not compile.
 */
public class ConstraintScopeOverloadTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "constraintscopeoverload",
        new String[] {
          "com/example/Simple.java",
          "com/example/sql/SqlNode.java",
          "com/example/sql/SqlOther.java",
          "com/example/sql/SqlParserPos.java",
          "com/example/util/Util.java"
        },
        new String[] {"com.example.Simple#toPos(Iterable<? extends SqlNode>)"});
  }
}
