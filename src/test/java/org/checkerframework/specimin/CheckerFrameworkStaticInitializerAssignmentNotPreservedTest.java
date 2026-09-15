package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Counterpart to {@link NullAwayStaticInitializerAssignmentPreservedTest} that uses the exact same
 * input, run under the default (Checker Framework/javac) modularity model instead of NullAway. This
 * demonstrates that following a static initializer block as a dependency really is gated on the
 * NullAway modularity model, and not general Specimin behavior: with the same {@code active} field
 * as the target, the block that assigns it is left alone here, so {@code active} falls through to
 * Slicer's ordinary "empty final field" repair and is given a default value instead of its real
 * one. Since the block is never followed, {@code ConfigLoader} -- which only that block would have
 * pulled in -- must not appear in the output at all. See {@link
 * org.checkerframework.specimin.modularity.ModularityModel#preserveStaticInitializerAssignments}.
 */
public class CheckerFrameworkStaticInitializerAssignmentNotPreservedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "checkerframework-static-initializer-assignment",
        new String[] {
          "org/example/config/ConfigCenter.java", "org/example/config/ConfigLoader.java"
        },
        new String[] {"org.example.config.ConfigCenter#active"});
  }
}
