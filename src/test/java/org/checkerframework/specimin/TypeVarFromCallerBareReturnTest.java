package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A calling method's type variable that reaches a synthetic method as its entire return type,
 * rather than nested inside one.
 *
 * <p>This is the same leak as {@link TypeVarFromCallerInstanceTest} -- {@code Holder} is not
 * generic, so there is no JLS 4.5.2 substitution channel and the copied {@code T} is unbound -- but
 * it exercises a distinct path once the method declares the type variable itself. A generated
 * method whose return type is one of its own type variables reports no type at all at a call site,
 * since the call site does not constrain it; that is deliberate, and {@link
 * org.checkerframework.specimin.unsolved.UnsolvedMethodAlternates#isOwnTypeVariable} exists to say
 * so. Code that reconciles a return statement with its method's declared return type has to
 * tolerate learning nothing, rather than treating it as a missing type.
 */
public class TypeVarFromCallerBareReturnTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "typevarfromcallerbarereturn",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Holder)"});
  }
}
