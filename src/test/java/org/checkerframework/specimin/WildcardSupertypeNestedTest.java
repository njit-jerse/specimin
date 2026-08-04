package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link WildcardSupertypeBoundedTest}, but the wildcard's bound is itself parameterized by a
 * wildcard ({@code ? extends List<?>}), so the replacement has to recurse: {@code
 * Baz<List<Object>>} is a subtype of {@code Baz<? extends List<?>>}, while {@code Baz<List<?>>} is
 * not even expressible in an extends clause.
 */
public class WildcardSupertypeNestedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "wildcardsupertypenested",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#target(Bar)"});
  }
}
