package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A lower-bounded wildcard {@code Baz<? super String>} constrains Baz's type <em>argument</em> to
 * be a supertype of String; it says nothing about Baz itself. Treating the bound as a supertype of
 * Baz is not just imprecise, it is uncompilable whenever the bound cannot be extended -- here
 * String is final, so {@code class Baz<T> extends String} does not compile.
 */
public class SuperWildcardFinalBoundTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "superwildcardfinalbound",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Bar)"});
  }
}
