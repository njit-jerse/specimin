package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin generates the right class for a fully-qualified static method call
 * whose class name is spelled in a way that the class-name-vs-constant-name convention does not
 * recognize: an acronym like {@code IOUtils}, or an all-lowercase name. JLS 6.5.2 settles both: the
 * leftmost identifier is not a variable in scope, so the qualifier of the method invocation is a
 * type name. Reduced from https://github.com/njit-jerse/specimin/issues/523.
 */
public class AcronymClassStaticMemberTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "acronymclassstaticmember",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
