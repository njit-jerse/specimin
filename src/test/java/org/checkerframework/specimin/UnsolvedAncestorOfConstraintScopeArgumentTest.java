package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A variant of {@link ConstraintArgOverloadTest} in which the overloaded call's argument has an
 * ordinary reference type, {@code Simple}, but one of {@code Simple}'s supertypes is not on the
 * source path. {@code Simple} is still known to be a {@code Listener}, so javac selects {@code
 * combine(Listener)} as the most specific overload (JLS 15.12.2.5), and its return type is what
 * lets {@code transform}'s {@code T} be inferred as {@code SqlParserPos}. Deciding that {@code
 * combine(Listener)} is applicable must therefore not depend on being able to enumerate every one
 * of {@code Simple}'s supertypes; if it does, only {@code combine(Object)} survives and the
 * target's return statement no longer compiles. Found while investigating issue #562.
 */
public class UnsolvedAncestorOfConstraintScopeArgumentTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedancestorofconstraintscopeargument",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#toPos(Iterable<? extends SqlNode>, Simple)"});
  }
}
