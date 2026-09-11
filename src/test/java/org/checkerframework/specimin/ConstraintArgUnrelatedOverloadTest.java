package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A variant of {@link ConstraintArgOverloadTest} in which the two overloads have <em>unrelated</em>
 * parameter types, so that only one of them is applicable at all.
 *
 * <p>{@code JavaParserUtil#couldArgumentBeTypeCompatibleWithParameterType} accepts any reference
 * parameter for a lambda constraint type, because JavaParser reports such a type's bound as a bare,
 * unbounded type variable that constrains nothing. Both overloads are therefore admitted as
 * applicable; neither is more specific than the other, since their parameter types are unrelated;
 * and the tie is broken by declaration order, which selects {@code combine(Unrelated)}. That drops
 * the only applicable overload from the output, so the call itself does not type-check.
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
