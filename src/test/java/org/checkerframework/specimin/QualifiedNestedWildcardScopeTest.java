package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test is {@link QualifiedNestedStaticFieldScopeTest}, but with {@code Outer} brought into
 * scope by a type-import-on-demand. {@code Outer} could then be either {@code com.other.Outer} or a
 * class in {@code com.example}, which would shadow the on-demand import (JLS 6.4.1). Either choice
 * is a correct program; what matters is that the synthetic type is placed in the class Specimin
 * generates for the scope. See issue #559.
 */
public class QualifiedNestedWildcardScopeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "qualifiednestedwildcardscope",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
