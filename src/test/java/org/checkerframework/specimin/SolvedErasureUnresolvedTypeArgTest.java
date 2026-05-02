package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that we do not generate extra symbols for unresolvable type arguments when the
 * erasure of the type is resolved. For example, no symbols (aside from the unresolved type arg) for
 * uses of List<Unsolved>.
 */
public class SolvedErasureUnresolvedTypeArgTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "solvederasureunsolvedtypearg",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
