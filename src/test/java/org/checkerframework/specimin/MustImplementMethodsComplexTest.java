package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.Test;

/**
 * This test checks if Specimin correctly preserves methods that are part of a JDK interface with an
 * indirect type parameter map (i.e. String --> E to E --> T).
 */
public class MustImplementMethodsComplexTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "mustimplementmethodscomplex",
        new String[] {"com/example/Foo.java"},
        new String[] {"com.example.Foo#foo()"});
  }
}
