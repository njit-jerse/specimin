package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test is {@link QualifiedNestedStaticFieldScopeTest}, but for a static method: the return
 * type synthesized for {@code Outer.Inner.foo()} must be placed relative to the fully-qualified
 * {@code com.other.Outer}, not a package named {@code Outer}. See issue #559.
 */
public class QualifiedNestedStaticMethodScopeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "qualifiednestedstaticmethodscope",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
