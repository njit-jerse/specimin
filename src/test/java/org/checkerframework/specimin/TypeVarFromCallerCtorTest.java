package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The same leak as {@link StaticTypeVarFromCallerTest}, but through a synthetic
 * <em>constructor</em> rather than a method.
 *
 * <p>Constructor parameter types are built from the argument types at the call site by the same
 * path that method parameter types are, so a calling method's type variable reaches a generated
 * constructor's signature the same way. {@code Holder} is not generic, so there is no substitution
 * channel (JLS 4.5.2) and the name is unbound.
 *
 * <p>The remedy is a generic constructor (JLS 8.8.4), which is why this fixture is worth keeping
 * separate: it is the only one whose expected output exercises a type parameter section on a
 * declaration that has no return type.
 */
public class TypeVarFromCallerCtorTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarfromcallerctor",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Src<T>)"});
  }
}
