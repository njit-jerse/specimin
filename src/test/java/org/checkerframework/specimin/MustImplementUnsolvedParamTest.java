package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A variant of {@link AbstractMethodUnsolvedParamTest} in which the class that implements the
 * interface is not the target: the target only calls the interface method and instantiates the
 * implementing class. {@code LeaseExistsRule} therefore enters the slice as a type declaration, and
 * JLS 8.1.1.1 requires a non-abstract class to implement every abstract method of its
 * superinterfaces -- so {@code LeaseExistsRule#apply} must be preserved (with a stubbed body) or
 * the output does not compile.
 *
 * <p>Specimin used to crash on this input for the same reason as {@link
 * AbstractMethodUnsolvedParamTest}. Merely suppressing that crash is not enough: the machinery that
 * finds must-implement methods also matches methods by signature, so the implementation is dropped
 * and the output fails to compile.
 */
public class MustImplementUnsolvedParamTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "mustimplementunsolvedparam",
        new String[] {"com/example/Target.java"},
        new String[] {"com.example.Target#target(InstanceInfo)"});
  }
}
