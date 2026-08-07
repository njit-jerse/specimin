package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks what Specimin does when a synthetic type is has one of its fields is used as an
 * annotation argument and is instantiated. An enum constructor may not be public, so if it created
 * as an enum then it would not compile.
 */
public class SyntheticEnumWithConstructorTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "syntheticenumwithconstructor",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target()"});
  }
}
