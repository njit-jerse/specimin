package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Reproduces issue 522. Resolving the left-hand side of {@code handlers = in} produces a
 * ResolvedArrayType, and Specimin used to name it by its description -- the source form {@code
 * com.example.Foo<?>[]} -- which is not an erased FQN and, because of the wildcard, is rejected
 * outright by FullyQualifiedNameSet.
 */
public class Issue522Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue522",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#Simple(Foo<?>[])"});
  }
}
