package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test is {@link QualifiedNestedStaticFieldScopeTest}, but with {@code Outer} unimported, so
 * that (absent any other declaration in scope) it names a class in the target's own package (JLS
 * 7.4.1, 6.4.1). See issue #559.
 */
public class QualifiedNestedSamePackageScopeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "qualifiednestedsamepackagescope",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
