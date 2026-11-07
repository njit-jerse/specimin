package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin qualifies a static field's scope before naming the field's
 * synthetic type, when the scope is a nested class written as {@code Outer.Inner} and {@code Outer}
 * comes from an import. {@code Outer} there is a simple type name (JLS 6.5.5.1), not a package, so
 * the synthetic type has to live in {@code com.other.Outer}: placing it in a package named {@code
 * Outer} makes the reference {@code Outer.X} inside {@code com.other.Outer} unresolvable, because
 * the class {@code Outer} obscures the package (JLS 6.4.2). See issue #559.
 */
public class QualifiedNestedStaticFieldScopeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "qualifiednestedstaticfieldscope",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
