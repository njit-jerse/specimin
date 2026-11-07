package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Companion to {@link NullAwayStaticInitializerAssignmentPreservedTest}: the target field, {@code
 * active}, is assigned in one static initializer block, and an unrelated field, {@code
 * buildFingerprint}, is assigned in its own separate static initializer block. Since {@code
 * buildFingerprint} is never the target and nothing reachable from the target reads it, neither
 * that field, its static block, nor the factory method it alone depends on should survive slicing
 * under NullAway. This checks that following a static initializer block as a dependency is
 * precise: it must find the specific block that assigns the target field, not sweep in every
 * static block in the class. See {@link CheckerFrameworkStaticInitializersNotPreservedTest} for
 * the same input under the default modularity model, where neither block is followed.
 */
public class NullAwayIgnoresUnrelatedStaticInitializerTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runNullAwayTestWithoutJarPaths(
        "nullaway-static-initializer-precision",
        new String[] {
          "org/example/telemetry/Telemetry.java", "org/example/telemetry/TelemetryFactory.java"
        },
        new String[] {"org.example.telemetry.Telemetry#active"});
  }
}
