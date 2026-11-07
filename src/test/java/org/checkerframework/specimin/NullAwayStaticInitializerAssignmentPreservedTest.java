package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that when running with the NullAway modularity model, a {@code static final}
 * field with no declaration-site initializer -- assigned instead in a static initializer block,
 * conditionally, via calls to another class -- has that block followed as a dependency, instead of
 * Specimin's "empty final field" repair inventing a default value for it. See {@link
 * org.checkerframework.specimin.modularity.ModularityModel#preserveStaticInitializerAssignments}
 * for why this is NullAway-specific, and {@link
 * CheckerFrameworkStaticInitializerAssignmentNotPreservedTest} for the same input under the
 * default modularity model, where the block is left alone instead.
 */
public class NullAwayStaticInitializerAssignmentPreservedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runNullAwayTestWithoutJarPaths(
        "nullaway-static-initializer-assignment",
        new String[] {
          "org/example/config/ConfigCenter.java", "org/example/config/ConfigLoader.java"
        },
        new String[] {"org.example.config.ConfigCenter#active"});
  }
}
