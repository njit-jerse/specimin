package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks what Specimin does when a synthetic type is both inferred to be an enum (because
 * one of its fields is used as an annotation argument) and given a constructor (because it is
 * instantiated). An enum constructor may not be public, so a naive rendering does not compile.
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
