package org.checkerframework.specimin;

import java.io.IOException;
import org.junit.jupiter.api.Test;

/**
 * Like {@link UnresolvableTypeArgumentTest}, but the unresolvable type argument is nested inside
 * another in-project type argument. Both {@code Container} and {@code Box} must be preserved. This
 * is the shape reported in Specimin issue #527.
 */
public class UnresolvableTypeArgumentNestedTest {
  @Test
  public void runTest() throws IOException {
    SpeciminTestExecutor.runTestWithoutJarPaths(
        "unresolvabletypeargumentnested",
        new String[] {"com/example/Simple.java"},
        new String[] {"com.example.Simple#bar()"});
  }
}
