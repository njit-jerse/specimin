package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A constructor call to a fully-known (JDK) class whose argument's type is unsolvable used to crash
 * Specimin with "Scope not created when it should've been", because it was treated as a call to a
 * synthetic constructor. This test exercises that case; the minimized output must compile.
 */
public class JdkCtorUnsolvedArgTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "jdkctorunsolvedarg",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target()"});
  }
}
