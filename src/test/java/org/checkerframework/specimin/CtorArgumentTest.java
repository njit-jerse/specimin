package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that UnsolvedSymbolVisitor doesn't skip unsolved symbols in the arguments to
 * constructors.
 */
public class CtorArgumentTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "ctorargument",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar(MethodGen)"});
  }
}
