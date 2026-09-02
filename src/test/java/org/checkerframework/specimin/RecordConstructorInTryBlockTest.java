package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that constructing a record inside a try block does not crash Specimin when the
 * constructor call has an unresolvable argument.
 *
 * <p>A canonical constructor is implicitly declared (JLS 8.10.4), so JavaParser has no {@code
 * ConstructorDeclaration} node for it and Specimin answers the constructor call with the record's
 * own declaration instead. Collecting the exceptions a try block can throw used to narrow that
 * answer to a {@code ResolvedConstructorDeclaration} without checking. The unresolvable argument is
 * what makes this reachable: without it the call resolves normally and the widening never happens.
 */
public class RecordConstructorInTryBlockTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "recordconstructorintryblock",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#foo()"});
  }
}
