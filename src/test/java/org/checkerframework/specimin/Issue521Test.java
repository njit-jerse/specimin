package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Regression test case for <a href="https://github.com/njit-jerse/specimin/issues/521">issue
 * 521</a>.
 */
public class Issue521Test {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "issue521",
        new String[] {"de/gurkenlabs/litiengine/entities/Creature.java"},
        new String[] {"de.gurkenlabs.litiengine.entities.Creature#Creature(String)"});
  }
}
