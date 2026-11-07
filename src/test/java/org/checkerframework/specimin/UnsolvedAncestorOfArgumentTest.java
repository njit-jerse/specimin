package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin does not crash when choosing an overload for a call whose argument
 * is resolvable but has an unsolvable supertype (here, {@code this}, whose class implements an
 * interface that is not on the source path). Deciding whether {@code Simple} can be passed to a
 * {@code Listener} parameter (JLS 15.12.2.2) requires walking {@code Simple}'s supertypes, which
 * JavaParser cannot complete. Derived from issue #562.
 */
public class UnsolvedAncestorOfArgumentTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedancestorofargument",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(Lifecycle)"});
  }
}
