package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A variant of {@link DanglingUnconstrainedReturnTypeTest} in which the synthetic method's
 * declaring type is generic, so that the class and the method each have a type variable.
 *
 * <p>Specimin names class and method type variables from the same sequence ({@code T}, {@code T1},
 * ...), so both are called {@code T} here, and the method's {@code T} shadows the class's. That is
 * why {@code UnsolvedMethodAlternates#isOwnTypeVariable}, which distinguishes a method's own type
 * variables by name, reads the return type as the method's own: under shadowing that is the correct
 * reading, because a method's type parameter hides the class's throughout the method's signature.
 *
 * <p>This test exists to catch a future change that makes that no longer true. If the two kinds of
 * type variable are ever given separate namespaces, or a class type variable can otherwise reach a
 * method's return type without shadowing, the name comparison would no longer be sound and this
 * fixture's output would change.
 */
public class UnconstrainedReturnInGenericClassTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unconstrainedreturningenericclass",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Foo<Baz>)"});
  }
}
