package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * This test checks that Specimin does not put a field named after a package into a synthetic
 * supertype. The enclosing qualified name {@code java.util.UUID} resolves, so by JLS 6.5.2 {@code
 * java} is a package name and not a field; declaring a field {@code java} in a supertype would
 * obscure the package (JLS 6.4.2) and stop the output from compiling.
 */
public class PackageNameObscuredBySupertypeTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "packagenameobscuredbysupertype",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#bar()"});
  }
}
