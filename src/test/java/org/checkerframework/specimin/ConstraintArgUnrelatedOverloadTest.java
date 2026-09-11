package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A variant of {@link ConstraintArgOverloadTest} in which the two overloads have <em>unrelated</em>
 * parameter types, so that only one of them is applicable at all.
 *
 * <p>JavaParser reports a lambda constraint's bound as a bare, unbounded type variable, so Specimin
 * cannot tell which is the right one. So, it has to treat both overloads as applicable; neither is
 * more specific than the other, since their parameter types are unrelated. This test's package
 * names are chosen in such a way that an arbitrary choice will favor the unrelated option, leading
 * to non-compiling output if Specimin doesn't have a fallback.
 */
public class ConstraintArgUnrelatedOverloadTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "constraintargunrelatedoverload",
        new String[] {
          "com/example/Simple.java",
          "com/example/sql/SqlNode.java",
          "com/example/sql/SqlOther.java",
          "com/example/sql/SqlParserPos.java",
          "com/example/other/Unrelated.java",
          "com/example/util/Util.java"
        },
        new String[] {"com.example.Simple#toPos(Iterable<? extends SqlNode>)"});
  }
}
