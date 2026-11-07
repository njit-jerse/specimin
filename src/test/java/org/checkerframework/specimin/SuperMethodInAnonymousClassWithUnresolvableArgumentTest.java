package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that a {@code super.m(...)} call inside an anonymous class keeps {@code m} in
 * the anonymous class's superclass (JLS 15.9.5, 15.12.1) when JavaParser cannot resolve the call
 * because an argument's type is unknown. The superclass is checked twice here, once as the anonymous
 * class's supertype and once as the type of {@code super}, so this also checks that the duplicate
 * candidate is not mistaken for an ambiguous overload.
 */
public class SuperMethodInAnonymousClassWithUnresolvableArgumentTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "supermethodinanonymousclasswithunresolvableargument",
        new String[] {"com/example/Outer.java"},
        new String[] {"com.example.Outer#foo(Factory,Info)"});
  }
}
