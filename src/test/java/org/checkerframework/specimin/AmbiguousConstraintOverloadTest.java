package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that when Specimin cannot tell two overloads apart, it preserves both rather than
 * committing to one.
 *
 * <p>The overloaded call's argument is a second lambda parameter, so its type is a lambda
 * constraint type, which carries no usable information: JavaParser reports its bound as a bare,
 * unbounded type variable. Both overloads are therefore admitted and neither is more specific, so
 * the {@link Resolver} API answers with an arbitrary one. In this example, that arbitrary pick is
 * not the overload javac selects, so preserving only it would leave the call with nothing to bind
 * to.
 *
 * <p>The expected output therefore keeps {@code combine(Unrelated)} and {@code Unrelated} and
 * {@code SqlOther} as well, none of which the call really needs. In the future, changing the
 * expected output to remove those expected files would be an improvement, not a regression.
 */
public class AmbiguousConstraintOverloadTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "ambiguousconstraintoverload",
        new String[] {
          "com/example/Simple.java",
          "com/example/sql/SqlNode.java",
          "com/example/sql/SqlOther.java",
          "com/example/sql/SqlParserPos.java",
          "com/example/other/Unrelated.java",
          "com/example/util/Util.java"
        },
        new String[] {
          "com.example.Simple#toPos(Iterable<? extends SqlNode>, Iterable<? extends SqlNode>)"
        });
  }
}
