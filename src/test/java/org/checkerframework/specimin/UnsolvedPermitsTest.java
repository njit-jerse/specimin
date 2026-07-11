package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that unsolved types in permits clauses are generated with a correct keyword (in
 * best effort run mode, this would be final, unless we have evidence otherwise).
 */
public class UnsolvedPermitsTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unsolvedpermits",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#foo()"});
  }
}
