package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks that an unsolved method reference with a known left-hand side (LHS) is generated
 * with the correct parameter types and voidness.
 */
public class UnsolvedMethodReferenceWithKnownLHSTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedmethodreferencewithknownlhs",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#foo()"});
  }
}
