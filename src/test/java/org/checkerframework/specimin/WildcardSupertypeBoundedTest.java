package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link WildcardSupertypeTest}, but for bounded wildcards. A wildcard cannot appear in an
 * extends clause, so it must be replaced by a concrete type argument; the bound itself is such a
 * type, since {@code Baz<Thing>} is a subtype of both {@code Baz<? extends Thing>} and {@code Baz<?
 * super Thing>}.
 */
public class WildcardSupertypeBoundedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "wildcardsupertypebounded",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Bar)"});
  }
}
