package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A static method's own type variable, inside a generic class whose type parameter it may not name.
 *
 * <p>By JLS 8.1.2 the call site cannot refer to {@code Simple}'s {@code C}, but {@code target}'s
 * own {@code T} is unaffected -- JLS 8.4.4 puts a method's type parameters in scope throughout its
 * declaration, static or not. This guards the obvious way to get JLS 8.1.2 wrong: stopping the walk
 * at a static method rather than excluding only the enclosing type's parameters. Doing that would
 * drop {@code T}, and {@code Box.from} would be left referring to a type variable it does not
 * declare.
 */
public class TypeVarFromCallerStaticScopeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarfromcallerstaticscope",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Src<T>)"});
  }
}
