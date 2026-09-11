package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A variant of {@link ConstraintScopeOverloadTest} in which the overloaded call's argument is
 * itself the lambda parameter, so its type is a lambda constraint type rather than an ordinary
 * reference type. Without Specimin's conservative logic for what arguments could be compatible with
 * parameters, we might reject {@code combine(SqlNode)} and leaves only {@code combine(Object)},
 * which would make the target method's return statement not compile.
 */
public class ConstraintArgOverloadTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "constraintargoverload",
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
