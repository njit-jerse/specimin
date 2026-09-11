package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link AndroidComponentsStaticBlockTest}, but with a second {@code static final} field
 * ({@code bootDiagnostics}) that is assigned in its own, separate static initializer block. Since
 * that field is never the target and nothing reachable from the target reads it, neither the
 * field, its static block, nor the detector method it alone depends on should survive slicing.
 * This checks that following a static initializer block as a dependency is precise: it must find
 * the specific block that assigns the target field, not sweep in every static block in the class.
 */
public class PlatformServicesStaticBlockTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "platformservicesstaticblock",
        new String[] {
          "org/example/runtime/PlatformServices.java", "org/example/runtime/PlatformDetector.java"
        },
        new String[] {"org.example.runtime.PlatformServices#instance"});
  }
}
