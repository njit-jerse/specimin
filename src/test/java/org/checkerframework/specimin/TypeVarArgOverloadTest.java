package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that an overload admitted only by {@code
 * JavaParserUtil#couldArgumentBeTypeCompatibleWithParameterType}'s accommodation loses to one that
 * the argument is genuinely assignable to.
 *
 * <p>The argument's type here is the type variable {@code T}, so the accommodation admits {@code
 * combine(SqlNode)} even though {@code T} is not a {@code SqlNode}. That candidate then
 * <em>wins</em> on specificity, because {@code SqlNode} is a subtype of {@code Object}: JLS
 * 15.12.2.5 is being applied to a candidate that JLS 15.12.2.2 should have excluded. javac selects
 * {@code combine(Object)}, and preserving {@code combine(SqlNode)} instead leaves the call unable
 * to compile.
 */
public class TypeVarArgOverloadTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarargoverload",
        new String[] {
          "com/example/Simple.java",
          "com/example/sql/SqlNode.java",
          "com/example/sql/SqlOther.java",
          "com/example/sql/SqlParserPos.java",
          "com/example/util/Util.java"
        },
        new String[] {"com.example.Simple#toPos(Iterable<? extends SqlNode>, T)"});
  }
}
