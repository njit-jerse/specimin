package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * A lower-bounded wildcard {@code Baz<? super String>} constrains Baz's type <em>argument</em> to
 * be a supertype of String; it says nothing about Baz itself. An earlier version of Specimin
 * incorrectly treated such a bound as applying to Baz; this is a regression test for that bug.
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
