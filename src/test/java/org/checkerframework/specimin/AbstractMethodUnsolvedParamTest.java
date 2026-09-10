package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * The target method implements an interface method whose parameter type cannot be resolved, because
 * the type is declared outside the root. Specimin used to crash with an {@code
 * UnsolvedSymbolException} here, because it identified abstract methods by their qualified
 * signature, and computing a qualified signature requires resolving every parameter type. This is a
 * minimization of https://github.com/njit-jerse/specimin/issues/547.
 *
 * <p>The abstract declaration in {@code Rule} is preserved: the target method overrides it, so the
 * override-compatibility rules of JLS 8.4.8.1 and 8.4.8.3 read it when type-checking the target.
 */
public class AbstractMethodUnsolvedParamTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "abstractmethodunsolvedparam",
        new String[] {"com/example/LeaseExistsRule.java"},
        new String[] {"com.example.LeaseExistsRule#apply(InstanceInfo)"});
  }
}
