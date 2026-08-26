package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a synthetic constructor's parameter type is the type that the argument
 * expression turns out to have, rather than the placeholder Specimin invents before it knows.
 * {@code Helper.make()} has no declared type to read, so its result starts as a placeholder; the
 * assignment on the next line settles it as {@code String}. The constructor must end up taking
 * {@code String}, and no placeholder class may be left in the output.
 */
public class ConstructorParamPlaceholderReplacedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "constructorparamplaceholderreplaced",
        new String[] {"example/Target.java"},
        new String[] {"example.Target#target()"});
  }
}
