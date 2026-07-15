package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that methods are properly preserved even if it is never called, in these cases:
 *
 * <p>Given:
 *
 * <ul>
 *   <li>Class A implements C, and class B implements C.
 *   <li>C declares foo(), so both A and B must implement foo().
 *   <li>A uses B, but never calls B.foo(). However, A.foo() is used.
 *   <li>Therefore, B.foo() must also be preserved because it is required to satisfy the interface
 *       implementation.
 * </ul>
 */
public class MustImplementMethodAdjacentClassTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "mustimplementmethodsadjacentclass",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#foo()"});
  }
}
