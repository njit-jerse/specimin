package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Checks that Specimin synthesizes a static method (and the class that its result is chained
 * against) when the receiver class is unsolved and lives in a package whose name begins with
 * "javax.". Such packages are not necessarily part of the JDK: javax.ws.rs.core, used here, comes
 * from JAX-RS.
 */
public class JavaxStaticMethodTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "javaxstaticmethod",
        new String[] {"org/example/Target.java"},
        new String[] {"org.example.Target#target()"});
  }
}
