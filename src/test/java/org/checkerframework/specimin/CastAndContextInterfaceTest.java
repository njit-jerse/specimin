package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link CastAndContextHarmlessTest}, but the cast target is an interface rather than {@code
 * Object}. A non-final class may always be cast to an unrelated interface, so returning {@code Bar}
 * satisfies both use sites; there is no need to widen the return type to {@code Object}.
 */
public class CastAndContextInterfaceTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "castandcontextinterface",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo)"});
  }
}
