package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Counterpart to {@link NullAwayIgnoresUnrelatedStaticInitializerTest} that uses the exact same
 * input, run under the default (Checker Framework/javac) modularity model instead of NullAway.
 * With the static-initializer-following logic gated off, the presence of a second, unrelated
 * static block (assigning {@code buildFingerprint}) makes no difference: neither block is
 * followed, so the target field {@code active} falls through to Slicer's ordinary "empty final
 * field" repair, just as it does with only one static block present in {@link
 * CheckerFrameworkStaticInitializerAssignmentNotPreservedTest}. Since neither block is followed,
 * {@code TelemetryFactory} must not appear in the output at all. See {@link
 * org.checkerframework.specimin.modularity.ModularityModel#preserveStaticInitializerAssignments}.
 */
public class CheckerFrameworkStaticInitializersNotPreservedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "checkerframework-static-initializer-precision",
        new String[] {
          "org/example/telemetry/Telemetry.java", "org/example/telemetry/TelemetryFactory.java"
        },
        new String[] {"org.example.telemetry.Telemetry#active"});
  }
}
